package com.satyam.splitly.settlement;

import com.satyam.splitly.common.exception.BadRequestException;
import com.satyam.splitly.common.exception.NotFoundException;
import com.satyam.splitly.common.money.MoneyUtils;
import com.satyam.splitly.group.Group;
import com.satyam.splitly.group.GroupAccess;
import com.satyam.splitly.group.GroupMember;
import com.satyam.splitly.group.GroupMemberRepository;
import com.satyam.splitly.group.GroupRepository;
import com.satyam.splitly.settlement.dto.CreateSettlementRequest;
import com.satyam.splitly.user.User;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recording money that actually changed hands.
 *
 * <p>A settlement is not a negative expense. An expense answers "what did this trip
 * cost?"; a settlement answers "who has paid whom back?". Modelling the second as the
 * first would corrupt every spending total in the group, which is why they are separate
 * tables and separate terms in the balance formula.
 */
@Service
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupAccess groupAccess;

    public SettlementService(SettlementRepository settlementRepository,
                             GroupRepository groupRepository,
                             GroupMemberRepository groupMemberRepository,
                             GroupAccess groupAccess) {
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupAccess = groupAccess;
    }

    @Transactional
    public Settlement record(Long groupId, Long callerId, CreateSettlementRequest request) {
        groupAccess.requireMember(groupId, callerId);

        if (request.fromUserId().equals(request.toUserId())) {
            throw new BadRequestException("A settlement needs two different people");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> NotFoundException.of("Group", groupId));

        Map<Long, User> members = groupMemberRepository.findByGroupIdWithUser(groupId).stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity()));

        User from = requireMember(members, request.fromUserId(), "Payer");
        User to = requireMember(members, request.toUserId(), "Recipient");

        // Any member may record a settlement, not only the two involved - in practice
        // whoever has the app open enters it. Overpaying is allowed too: it simply flips
        // the balance the other way, which is what really happens when someone rounds up.
        return settlementRepository.save(new Settlement(
                group, from, to, MoneyUtils.normalise(request.amount()), request.note()));
    }

    public List<Settlement> list(Long groupId, Long callerId) {
        groupAccess.requireMember(groupId, callerId);
        return settlementRepository.findByGroupIdWithUsers(groupId);
    }

    private User requireMember(Map<Long, User> members, Long userId, String role) {
        User user = members.get(userId);
        if (user == null) {
            throw new BadRequestException(role + " " + userId + " is not a member of this group");
        }
        return user;
    }
}
