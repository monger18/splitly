package com.satyam.splitly.balance;

import com.satyam.splitly.balance.dto.GroupBalanceResponse;
import com.satyam.splitly.balance.dto.MemberBalanceResponse;
import com.satyam.splitly.balance.dto.TransferResponse;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.expense.ExpenseRepository;
import com.satyam.splitly.expense.ExpenseShareRepository;
import com.satyam.splitly.group.Group;
import com.satyam.splitly.group.GroupAccess;
import com.satyam.splitly.group.GroupMember;
import com.satyam.splitly.group.GroupMemberRepository;
import com.satyam.splitly.settlement.SettlementRepository;
import com.satyam.splitly.user.User;
import com.satyam.splitly.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Works out who owes whom.
 *
 * <p><strong>Nothing here is stored.</strong> There is no balance column anywhere; every
 * number is recomputed from expenses, shares and settlements on each read. A stored
 * balance would have to be updated correctly by every write path - creating an expense,
 * editing one, soft-deleting one, recording a settlement, removing a member - and the
 * first path that forgets leaves a wrong number that nothing detects. Deriving it means
 * the answer cannot drift.
 *
 * <p>The cost is four aggregate queries per group, which the database answers from
 * indexes. Caching that result in Redis, invalidated on write, is phase 6 - and caching a
 * correct calculation is a much safer optimisation than trying to maintain a stored one.
 *
 * <p>The formula, per member:
 * <pre>
 *   net = what they paid out for expenses
 *       - what they owe as their share of expenses
 *       + what they have since handed over in settlements
 *       - what they have received in settlements
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupAccess groupAccess;

    public BalanceService(ExpenseRepository expenseRepository,
                          ExpenseShareRepository expenseShareRepository,
                          SettlementRepository settlementRepository,
                          GroupMemberRepository groupMemberRepository,
                          UserRepository userRepository,
                          GroupAccess groupAccess) {
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.settlementRepository = settlementRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupAccess = groupAccess;
    }

    /**
     * Net position of every member of a group, and of anyone who has left but still has
     * activity recorded against them.
     *
     * <p>No access check: this is the internal calculation, called by other services that
     * have already established the caller's rights. The public endpoints below check.
     */
    public Map<Long, BigDecimal> netByUser(Long groupId) {
        Map<Long, BigDecimal> net = new LinkedHashMap<>();

        // Seed with every current member so someone with no activity still appears at 0.
        groupMemberRepository.findByGroupIdWithUser(groupId)
                .forEach(member -> net.put(member.getUser().getId(), BigDecimal.ZERO));

        apply(net, expenseRepository.sumPaidByUser(groupId), true);
        apply(net, expenseShareRepository.sumOwedByUser(groupId), false);
        apply(net, settlementRepository.sumPaidOutByUser(groupId), true);
        apply(net, settlementRepository.sumReceivedByUser(groupId), false);

        net.replaceAll((userId, amount) -> MoneyUtils.normalise(amount));
        return net;
    }

    /** One member's net. Used by group membership rules; assumes access is already checked. */
    public BigDecimal netFor(Long groupId, Long userId) {
        return netByUser(groupId).getOrDefault(userId, BigDecimal.ZERO);
    }

    public List<MemberBalanceResponse> groupBalances(Long groupId, Long callerId) {
        groupAccess.requireMember(groupId, callerId);

        Map<Long, BigDecimal> net = netByUser(groupId);
        Map<Long, User> users = usersById(net.keySet());

        return net.entrySet().stream()
                .map(entry -> {
                    User user = users.get(entry.getKey());
                    return new MemberBalanceResponse(entry.getKey(),
                            user == null ? "(removed user)" : user.getName(),
                            user == null ? null : user.getEmail(),
                            entry.getValue());
                })
                // Largest creditor first, so the person owed the most is at the top.
                .sorted(Comparator.comparing(MemberBalanceResponse::net).reversed())
                .toList();
    }

    /** The short list of payments that would clear the group. */
    public List<TransferResponse> simplified(Long groupId, Long callerId) {
        groupAccess.requireMember(groupId, callerId);

        List<Transfer> transfers = DebtSimplifier.simplify(netByUser(groupId));
        Map<Long, User> users = usersById(transfers.stream()
                .flatMap(transfer -> java.util.stream.Stream.of(transfer.fromUserId(), transfer.toUserId()))
                .collect(Collectors.toSet()));

        return transfers.stream()
                .map(transfer -> new TransferResponse(
                        transfer.fromUserId(), nameOf(users, transfer.fromUserId()),
                        transfer.toUserId(), nameOf(users, transfer.toUserId()),
                        transfer.amount()))
                .toList();
    }

    /**
     * The caller's net in every group they belong to.
     *
     * <p>Four grouped queries in total, not four per group - each one aggregates by group
     * for this one user. Looping over the groups and calling {@link #netByUser} would be
     * the obvious implementation and would issue four queries per group.
     */
    public List<GroupBalanceResponse> myBalances(Long userId) {
        Map<Long, Group> groups = groupMemberRepository.findByUserIdWithGroup(userId).stream()
                .map(GroupMember::getGroup)
                .collect(Collectors.toMap(Group::getId, Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));

        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        groups.keySet().forEach(groupId -> net.put(groupId, BigDecimal.ZERO));

        applyByGroup(net, expenseRepository.sumPaidByGroupForUser(userId), true);
        applyByGroup(net, expenseShareRepository.sumOwedByGroupForUser(userId), false);
        applyByGroup(net, settlementRepository.sumPaidOutByGroupForUser(userId), true);
        applyByGroup(net, settlementRepository.sumReceivedByGroupForUser(userId), false);

        List<GroupBalanceResponse> result = new ArrayList<>();
        groups.forEach((groupId, group) -> result.add(new GroupBalanceResponse(
                groupId, group.getName(), group.getCurrency(),
                MoneyUtils.normalise(net.getOrDefault(groupId, BigDecimal.ZERO)))));
        return result;
    }

    private void apply(Map<Long, BigDecimal> net, List<UserAmount> rows, boolean add) {
        rows.forEach(row -> net.merge(row.userId(),
                add ? row.amount() : row.amount().negate(), BigDecimal::add));
    }

    private void applyByGroup(Map<Long, BigDecimal> net, List<GroupAmount> rows, boolean add) {
        rows.forEach(row -> {
            if (net.containsKey(row.groupId())) {   // only groups the caller still belongs to
                net.merge(row.groupId(), add ? row.amount() : row.amount().negate(), BigDecimal::add);
            }
        });
    }

    private Map<Long, User> usersById(java.util.Collection<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String nameOf(Map<Long, User> users, Long userId) {
        User user = users.get(userId);
        return user == null ? "(removed user)" : user.getName();
    }
}
