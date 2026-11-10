package ru.skinemsya.expense;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skinemsya.common.ApiException;
import ru.skinemsya.core.Expense;
import ru.skinemsya.core.Money;
import ru.skinemsya.core.Split;
import ru.skinemsya.group.ExpenseGroup;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.group.GroupService;
import ru.skinemsya.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ExpenseService {

    private final GroupService groupService;
    private final GroupExpenseRepository expenses;
    private final UserRepository users;

    public ExpenseService(GroupService groupService, GroupExpenseRepository expenses, UserRepository users) {
        this.groupService = groupService;
        this.expenses = expenses;
        this.users = users;
    }

    public ExpenseResponse create(Long userId, Long groupId, CreateExpenseRequest request) {
        ExpenseGroup group = groupService.requireAccess(userId, groupId);
        Map<Long, GroupMember> members = groupService.membersById(groupId);
        GroupMember payer = requireMember(members, request.payerId());

        Expense<Long> expense = buildExpense(payer.getId(), toMoney(request.amount()), request.split(), members);

        Map<Long, Long> shares = new LinkedHashMap<>();
        expense.shares().forEach((memberId, share) -> shares.put(memberId, share.kopecks()));

        GroupExpense saved = expenses.save(new GroupExpense(
                group,
                payer,
                request.description().trim(),
                expense.amount().kopecks(),
                request.split().type(),
                request.spentOn() != null ? request.spentOn() : LocalDate.now(),
                users.getReferenceById(userId),
                shares));
        return ExpenseResponse.from(saved, members);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Long userId, Long groupId) {
        groupService.requireAccess(userId, groupId);
        Map<Long, GroupMember> members = groupService.membersById(groupId);
        return expenses.findAllWithShares(groupId).stream()
                .map(expense -> ExpenseResponse.from(expense, members))
                .toList();
    }

    public void delete(Long userId, Long groupId, Long expenseId) {
        groupService.requireAccess(userId, groupId);
        GroupExpense expense = expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> ApiException.notFound("Трата не найдена"));
        expenses.delete(expense);
    }

    /**
     * Переводит запрос в доменную модель. Ошибки ядра (доли не сходятся с суммой,
     * участник указан дважды и т.п.) превращаются в 400 Bad Request.
     */
    private Expense<Long> buildExpense(Long payerId, Money amount, SplitRequest split,
                                       Map<Long, GroupMember> members) {
        try {
            Split<Long> rule = switch (split.type()) {
                case EQUAL -> equalSplit(split, members);
                case EXACT -> exactSplit(split, members);
                case SHARES -> sharesSplit(split, members);
            };
            return Expense.of(payerId, amount, rule);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    private Split<Long> equalSplit(SplitRequest split, Map<Long, GroupMember> members) {
        List<Long> ids = split.memberIds() == null || split.memberIds().isEmpty()
                ? List.copyOf(members.keySet())
                : split.memberIds();
        ids.forEach(id -> requireMember(members, id));
        return new Split.Equal<>(ids);
    }

    private Split<Long> exactSplit(SplitRequest split, Map<Long, GroupMember> members) {
        Map<Long, Money> amounts = new LinkedHashMap<>();
        for (SplitPartRequest part : requireParts(split)) {
            requireMember(members, part.memberId());
            if (part.amount() == null) {
                throw ApiException.badRequest("Для деления точными суммами укажите amount у каждого участника");
            }
            if (amounts.put(part.memberId(), toMoney(part.amount())) != null) {
                throw ApiException.badRequest("Участник %d указан дважды".formatted(part.memberId()));
            }
        }
        return new Split.Exact<>(amounts);
    }

    private Split<Long> sharesSplit(SplitRequest split, Map<Long, GroupMember> members) {
        Map<Long, Integer> shares = new LinkedHashMap<>();
        for (SplitPartRequest part : requireParts(split)) {
            requireMember(members, part.memberId());
            if (part.share() == null) {
                throw ApiException.badRequest("Для деления по долям укажите share у каждого участника");
            }
            if (shares.put(part.memberId(), part.share()) != null) {
                throw ApiException.badRequest("Участник %d указан дважды".formatted(part.memberId()));
            }
        }
        return new Split.Shares<>(shares);
    }

    private static List<SplitPartRequest> requireParts(SplitRequest split) {
        if (split.parts() == null || split.parts().isEmpty()) {
            throw ApiException.badRequest("Укажите доли участников в поле parts");
        }
        return split.parts();
    }

    private static GroupMember requireMember(Map<Long, GroupMember> members, Long memberId) {
        GroupMember member = members.get(memberId);
        if (member == null) {
            throw ApiException.badRequest("Участник %d не состоит в группе".formatted(memberId));
        }
        return member;
    }

    private static Money toMoney(BigDecimal amount) {
        try {
            return Money.of(amount);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }
}
