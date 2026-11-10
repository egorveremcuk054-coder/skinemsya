package ru.skinemsya.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Жадный алгоритм взаимозачёта.
 * <p>
 * На каждом шаге самый большой должник переводит самому большому кредитору
 * меньшую из двух сумм. Каждый перевод обнуляет хотя бы одного участника,
 * поэтому переводов не больше {@code n − 1}. Сложность — {@code O(n log n)}.
 * <p>
 * Алгоритм быстрый, но не всегда находит абсолютный минимум переводов:
 * см. {@link OptimalDebtSimplifier}.
 */
public final class GreedyDebtSimplifier implements DebtSimplifier {

    private record Position<T>(T participant, long kopecks, int order) {
    }

    private static final Comparator<Position<?>> LARGEST_FIRST =
            Comparator.<Position<?>>comparingLong(Position::kopecks).reversed()
                    .thenComparingInt(Position::order);

    @Override
    public <P> List<Transfer<P>> simplify(Map<P, Money> balances) {
        DebtSimplifier.requireBalanced(balances);

        PriorityQueue<Position<P>> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Position<P>> debtors = new PriorityQueue<>(LARGEST_FIRST);
        int order = 0;
        for (Map.Entry<P, Money> entry : balances.entrySet()) {
            long kopecks = entry.getValue().kopecks();
            if (kopecks > 0) {
                creditors.add(new Position<>(entry.getKey(), kopecks, order));
            } else if (kopecks < 0) {
                debtors.add(new Position<>(entry.getKey(), -kopecks, order));
            }
            order++;
        }

        List<Transfer<P>> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Position<P> creditor = creditors.poll();
            Position<P> debtor = debtors.poll();
            long amount = Math.min(creditor.kopecks(), debtor.kopecks());

            transfers.add(new Transfer<>(debtor.participant(), creditor.participant(), Money.ofKopecks(amount)));

            if (creditor.kopecks() > amount) {
                creditors.add(new Position<>(creditor.participant(), creditor.kopecks() - amount, creditor.order()));
            }
            if (debtor.kopecks() > amount) {
                debtors.add(new Position<>(debtor.participant(), debtor.kopecks() - amount, debtor.order()));
            }
        }
        return List.copyOf(transfers);
    }
}
