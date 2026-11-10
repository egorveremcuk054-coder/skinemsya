package ru.skinemsya.expense;

import ru.skinemsya.core.Money;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.group.MemberRef;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        MemberRef payer,
        SplitType splitType,
        LocalDate spentOn,
        List<Share> shares,
        Instant createdAt
) {

    public record Share(MemberRef member, BigDecimal amount) {
    }

    /**
     * @param members участники группы по id — чтобы показать имена в долях
     */
    public static ExpenseResponse from(GroupExpense expense, Map<Long, GroupMember> members) {
        List<Share> shares = members.values().stream()
                .filter(member -> expense.getShares().containsKey(member.getId()))
                .map(member -> new Share(
                        MemberRef.from(member),
                        Money.ofKopecks(expense.getShares().get(member.getId())).toBigDecimal()))
                .toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                Money.ofKopecks(expense.getAmount()).toBigDecimal(),
                MemberRef.from(expense.getPayer()),
                expense.getSplitType(),
                expense.getSpentOn(),
                shares,
                expense.getCreatedAt());
    }
}
