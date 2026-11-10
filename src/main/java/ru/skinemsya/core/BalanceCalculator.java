package ru.skinemsya.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Считает баланс каждого участника группы.
 * <p>
 * Баланс = сколько участник заплатил − сколько он должен.
 * Положительный баланс — участнику должны, отрицательный — должен он.
 * Сумма всех балансов всегда равна нулю.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    /**
     * @param participants все участники группы (попадут в результат даже с нулевым балансом)
     * @param expenses     траты
     * @param settlements  уже сделанные переводы между участниками
     */
    public static <P> Map<P, Money> calculate(Collection<P> participants,
                                              Collection<Expense<P>> expenses,
                                              Collection<Transfer<P>> settlements) {
        Map<P, Money> balances = new LinkedHashMap<>();
        participants.forEach(p -> balances.put(p, Money.ZERO));

        for (Expense<P> expense : expenses) {
            balances.merge(expense.payer(), expense.amount(), Money::plus);
            expense.shares().forEach((participant, share) ->
                    balances.merge(participant, share.negate(), Money::plus));
        }
        for (Transfer<P> transfer : settlements) {
            balances.merge(transfer.from(), transfer.amount(), Money::plus);
            balances.merge(transfer.to(), transfer.amount().negate(), Money::plus);
        }
        return Collections.unmodifiableMap(balances);
    }
}
