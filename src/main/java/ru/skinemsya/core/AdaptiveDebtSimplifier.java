package ru.skinemsya.core;

import java.util.List;
import java.util.Map;

/**
 * Для небольших групп использует точный алгоритм, для больших — жадный.
 */
public final class AdaptiveDebtSimplifier implements DebtSimplifier {

    /** До скольких участников с ненулевым балансом считаем точно (2^16 состояний — мгновенно). */
    public static final int OPTIMAL_THRESHOLD = 16;

    private final OptimalDebtSimplifier optimal = new OptimalDebtSimplifier();
    private final GreedyDebtSimplifier greedy = new GreedyDebtSimplifier();

    @Override
    public <P> List<Transfer<P>> simplify(Map<P, Money> balances) {
        long nonZero = balances.values().stream().filter(b -> !b.isZero()).count();
        return nonZero <= OPTIMAL_THRESHOLD ? optimal.simplify(balances) : greedy.simplify(balances);
    }
}
