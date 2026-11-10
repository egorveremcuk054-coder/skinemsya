package ru.skinemsya.balance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skinemsya.core.BalanceCalculator;
import ru.skinemsya.core.DebtSimplifier;
import ru.skinemsya.core.Expense;
import ru.skinemsya.core.Money;
import ru.skinemsya.core.Transfer;
import ru.skinemsya.expense.GroupExpense;
import ru.skinemsya.expense.GroupExpenseRepository;
import ru.skinemsya.group.GroupMember;
import ru.skinemsya.group.GroupService;
import ru.skinemsya.group.MemberRef;
import ru.skinemsya.settlement.Settlement;
import ru.skinemsya.settlement.SettlementRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Связывает данные из БД с алгоритмами из ядра: считает балансы и предлагает переводы.
 */
@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final GroupService groupService;
    private final GroupExpenseRepository expenses;
    private final SettlementRepository settlements;
    private final DebtSimplifier debtSimplifier;

    public BalanceService(GroupService groupService, GroupExpenseRepository expenses,
                          SettlementRepository settlements, DebtSimplifier debtSimplifier) {
        this.groupService = groupService;
        this.expenses = expenses;
        this.settlements = settlements;
        this.debtSimplifier = debtSimplifier;
    }

    public List<BalanceResponse> balances(Long userId, Long groupId) {
        Ledger ledger = loadLedger(userId, groupId);
        return ledger.balances().entrySet().stream()
                .map(entry -> new BalanceResponse(ledger.ref(entry.getKey()), entry.getValue().toBigDecimal()))
                .toList();
    }

    public List<TransferResponse> settleUp(Long userId, Long groupId) {
        Ledger ledger = loadLedger(userId, groupId);
        return debtSimplifier.simplify(ledger.balances()).stream()
                .map(transfer -> new TransferResponse(
                        ledger.ref(transfer.from()),
                        ledger.ref(transfer.to()),
                        transfer.amount().toBigDecimal()))
                .toList();
    }

    private Ledger loadLedger(Long userId, Long groupId) {
        groupService.requireAccess(userId, groupId);
        Map<Long, GroupMember> members = groupService.membersById(groupId);

        List<Expense<Long>> groupExpenses = expenses.findAllWithShares(groupId).stream()
                .map(BalanceService::toDomain)
                .toList();
        List<Transfer<Long>> groupSettlements = settlements.findAllByGroup(groupId).stream()
                .map(BalanceService::toDomain)
                .toList();

        Map<Long, Money> balances = BalanceCalculator.calculate(members.keySet(), groupExpenses, groupSettlements);
        return new Ledger(members, balances);
    }

    private static Expense<Long> toDomain(GroupExpense expense) {
        Map<Long, Money> shares = new LinkedHashMap<>();
        expense.getShares().forEach((memberId, kopecks) -> shares.put(memberId, Money.ofKopecks(kopecks)));
        return new Expense<>(expense.getPayer().getId(), Money.ofKopecks(expense.getAmount()), shares);
    }

    private static Transfer<Long> toDomain(Settlement settlement) {
        return new Transfer<>(
                settlement.getFromMember().getId(),
                settlement.getToMember().getId(),
                Money.ofKopecks(settlement.getAmount()));
    }

    private record Ledger(Map<Long, GroupMember> members, Map<Long, Money> balances) {

        MemberRef ref(Long memberId) {
            return MemberRef.from(members.get(memberId));
        }
    }
}
