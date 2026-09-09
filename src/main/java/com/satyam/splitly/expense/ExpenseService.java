package com.satyam.splitly.expense;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.exception.ForbiddenException;
import com.satyam.splitly.common.exception.NotFoundException;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.dto.CreateExpenseRequest;
import com.satyam.splitly.expense.dto.ParticipantRequest;
import com.satyam.splitly.expense.split.SplitInput;
import com.satyam.splitly.expense.split.SplitShare;
import com.satyam.splitly.expense.split.SplitStrategyResolver;
import com.satyam.splitly.group.Group;
import com.satyam.splitly.group.GroupAccess;
import com.satyam.splitly.group.GroupMember;
import com.satyam.splitly.group.GroupRole;
import com.satyam.splitly.group.GroupMemberRepository;
import com.satyam.splitly.group.GroupRepository;
import com.satyam.splitly.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupAccess groupAccess;
    private final SplitStrategyResolver splitStrategyResolver;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupRepository groupRepository,
                          GroupMemberRepository groupMemberRepository,
                          GroupAccess groupAccess,
                          SplitStrategyResolver splitStrategyResolver) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupAccess = groupAccess;
        this.splitStrategyResolver = splitStrategyResolver;
    }

    @Transactional
    public Expense create(Long groupId, Long callerId, CreateExpenseRequest request) {
        groupAccess.requireMember(groupId, callerId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.of("Group", groupId));

        Map<Long, User> members = membersById(groupId);
        User payer = requireMember(members, request.paidBy(), "Payer");
        User creator = requireMember(members, callerId, "Creator");

        Expense expense = new Expense(group, request.description().trim(),
                MoneyUtils.normalise(request.amount()), payer,
                request.splitType(), request.expenseDate(), creator);

        expense.replaceShares(buildShares(request, members, expense.getAmount()));
        return expenseRepository.save(expense);
    }

    public Page<Expense> list(Long groupId, Long callerId, Pageable pageable) {
        groupAccess.requireMember(groupId, callerId);
        return expenseRepository.findPageByGroupId(groupId, pageable);
    }

    public Expense getDetail(Long expenseId, Long callerId) {
        Expense expense = expenseRepository.findDetailById(expenseId)
                .orElseThrow(() -> NotFoundException.of("Expense", expenseId));
        groupAccess.requireMember(expense.getGroup().getId(), callerId);
        return expense;
    }

    /**
     * Replacing an expense's contents rather than deleting and recreating it, so its id
     * and audit trail survive. {@code replaceShares} clears the collection, and
     * orphanRemoval deletes the rows that dropped out.
     */
    @Transactional
    public Expense update(Long expenseId, Long callerId, CreateExpenseRequest request) {
        Expense expense = loadEditable(expenseId, callerId);
        Long groupId = expense.getGroup().getId();

        Map<Long, User> members = membersById(groupId);
        User payer = requireMember(members, request.paidBy(), "Payer");

        expense.setDescription(request.description().trim());
        expense.setAmount(MoneyUtils.normalise(request.amount()));
        expense.setPaidBy(payer);
        expense.setSplitType(request.splitType());
        expense.setExpenseDate(request.expenseDate());

        // Delete the old share rows before inserting the new ones. Without this flush
        // Hibernate emits the inserts first and the unique index on
        // (expense_id, user_id) rejects any participant who is in both sets.
        expense.clearShares();
        expenseRepository.flush();
        expense.addShares(buildShares(request, members, expense.getAmount()));

        return expense;   // inside a transaction, the dirty entity is flushed automatically
    }

    /**
     * Soft delete. The row stays so that history and any past balance calculation remain
     * explainable; every read filters on {@code deletedAt is null}.
     */
    @Transactional
    public void delete(Long expenseId, Long callerId) {
        Expense expense = loadEditable(expenseId, callerId);
        expense.setDeletedAt(Instant.now());
    }

    /** Editable by whoever recorded the expense, or by a group admin. */
    private Expense loadEditable(Long expenseId, Long callerId) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(expenseId)
                .orElseThrow(() -> NotFoundException.of("Expense", expenseId));

        GroupMember membership = groupAccess.requireMember(expense.getGroup().getId(), callerId);
        boolean isCreator = expense.getCreatedBy().getId().equals(callerId);

        if (!isCreator && membership.getRole() != GroupRole.ADMIN) {
            throw new ForbiddenException("Only the person who added this expense, or a group admin, can change it");
        }
        return expense;
    }

    /**
     * Turns the request's participants into shares.
     *
     * <p>The chosen strategy does the arithmetic; this method's job is everything the
     * strategy cannot know about - that the people exist, belong to the group, and appear
     * only once.
     */
    private List<ExpenseShare> buildShares(CreateExpenseRequest request, Map<Long, User> members, BigDecimal amount) {
        Set<Long> seen = new HashSet<>();
        for (ParticipantRequest participant : request.participants()) {
            if (!seen.add(participant.userId())) {
                throw new BadRequestException("User " + participant.userId() + " appears twice in the split");
            }
            requireMember(members, participant.userId(), "Participant");
        }

        List<SplitInput> inputs = request.participants().stream()
                .map(participant -> new SplitInput(participant.userId(), participant.value()))
                .toList();

        List<SplitShare> shares = splitStrategyResolver.resolve(request.splitType()).split(amount, inputs);

        // The strategies guarantee this; asserting it here means a future strategy with a
        // rounding bug fails loudly at the write instead of quietly skewing every balance.
        BigDecimal total = MoneyUtils.sum(shares.stream().map(SplitShare::amount).toList());
        if (!MoneyUtils.equalAmounts(total, amount)) {
            throw new IllegalStateException(
                    "Split produced %s for an expense of %s".formatted(total, amount));
        }

        return shares.stream()
                .map(share -> new ExpenseShare(members.get(share.userId()), share.amount()))
                .toList();
    }

    private Map<Long, User> membersById(Long groupId) {
        return groupMemberRepository.findByGroupIdWithUser(groupId).stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private User requireMember(Map<Long, User> members, Long userId, String role) {
        User user = members.get(userId);
        if (user == null) {
            throw new BadRequestException(role + " " + userId + " is not a member of this group");
        }
        return user;
    }
}
