package ru.skinemsya.settlement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skinemsya.common.ApiException;
import ru.skinemsya.core.Money;
import ru.skinemsya.group.ExpenseGroup;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.group.GroupMemberRepository;
import ru.skinemsya.group.GroupService;
import ru.skinemsya.user.UserRepository;

import java.util.List;

@Service
@Transactional
public class SettlementService {

    private final GroupService groupService;
    private final GroupMemberRepository members;
    private final SettlementRepository settlements;
    private final UserRepository users;

    public SettlementService(GroupService groupService, GroupMemberRepository members,
                             SettlementRepository settlements, UserRepository users) {
        this.groupService = groupService;
        this.members = members;
        this.settlements = settlements;
        this.users = users;
    }

    public SettlementResponse create(Long userId, Long groupId, CreateSettlementRequest request) {
        ExpenseGroup group = groupService.requireAccess(userId, groupId);
        GroupMember from = requireMember(groupId, request.fromMemberId());
        GroupMember to = requireMember(groupId, request.toMemberId());
        if (from.getId().equals(to.getId())) {
            throw ApiException.badRequest("Нельзя перевести деньги самому себе");
        }

        Money amount;
        try {
            amount = Money.of(request.amount());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }

        Settlement settlement = settlements.save(
                new Settlement(group, from, to, amount.kopecks(), users.getReferenceById(userId)));
        return SettlementResponse.from(settlement);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> list(Long userId, Long groupId) {
        groupService.requireAccess(userId, groupId);
        return settlements.findAllByGroup(groupId).stream()
                .map(SettlementResponse::from)
                .toList();
    }

    public void delete(Long userId, Long groupId, Long settlementId) {
        groupService.requireAccess(userId, groupId);
        Settlement settlement = settlements.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(() -> ApiException.notFound("Перевод не найден"));
        settlements.delete(settlement);
    }

    private GroupMember requireMember(Long groupId, Long memberId) {
        return members.findByIdAndGroupId(memberId, groupId)
                .orElseThrow(() -> ApiException.badRequest("Участник %d не состоит в группе".formatted(memberId)));
    }
}
