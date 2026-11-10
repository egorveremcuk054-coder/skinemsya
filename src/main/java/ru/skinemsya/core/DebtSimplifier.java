package ru.skinemsya.core;

import java.util.List;
import java.util.Map;

/**
 * Превращает балансы участников в список переводов, после которых все в расчёте.
 */
public interface DebtSimplifier {

    /**
     * @param balances баланс каждого участника; сумма должна быть равна нулю
     * @return переводы, после которых баланс каждого участника станет нулевым
     * @throws IllegalArgumentException если сумма балансов не равна нулю
     */
    <P> List<Transfer<P>> simplify(Map<P, Money> balances);

    static void requireBalanced(Map<?, Money> balances) {
        Money total = Money.sum(balances.values());
        if (!total.isZero()) {
            throw new IllegalArgumentException("Сумма балансов должна быть равна нулю, а она равна " + total);
        }
    }
}
