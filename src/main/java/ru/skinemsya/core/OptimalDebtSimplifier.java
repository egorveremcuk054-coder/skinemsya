package ru.skinemsya.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Точный алгоритм: находит минимально возможное число переводов.
 * <p>
 * Идея: если участников можно разбить на {@code k} групп, в каждой из которых
 * сумма балансов равна нулю, то каждую группу из {@code m} человек можно закрыть
 * за {@code m − 1} перевод, а всего понадобится {@code n − k} переводов.
 * Значит, нужно найти разбиение на максимальное число групп с нулевой суммой.
 * <p>
 * Задача NP-трудная, поэтому решаем её динамикой по подмножествам за
 * {@code O(2^n · n)}: {@code best[mask]} — максимальное число нулевых групп,
 * на которые можно разбить участников из {@code mask}. Работает для
 * {@value #MAX_PARTICIPANTS} участников с ненулевым балансом.
 */
public final class OptimalDebtSimplifier implements DebtSimplifier {

    public static final int MAX_PARTICIPANTS = 20;

    private final GreedyDebtSimplifier greedy = new GreedyDebtSimplifier();

    @Override
    public <P> List<Transfer<P>> simplify(Map<P, Money> balances) {
        DebtSimplifier.requireBalanced(balances);

        List<P> people = new ArrayList<>();
        List<Long> amountList = new ArrayList<>();
        balances.forEach((participant, balance) -> {
            if (!balance.isZero()) {
                people.add(participant);
                amountList.add(balance.kopecks());
            }
        });

        int n = people.size();
        if (n == 0) {
            return List.of();
        }
        if (n > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException(
                    "Точный алгоритм работает не больше чем для %d участников, а их %d"
                            .formatted(MAX_PARTICIPANTS, n));
        }
        long[] amounts = amountList.stream().mapToLong(Long::longValue).toArray();

        int full = (1 << n) - 1;
        long[] sum = new long[full + 1];
        byte[] best = new byte[full + 1];
        for (int mask = 1; mask <= full; mask++) {
            int lowest = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + amounts[lowest];

            int bestWithoutOne = 0;
            for (int rest = mask; rest != 0; rest &= rest - 1) {
                int bit = rest & -rest;
                bestWithoutOne = Math.max(bestWithoutOne, best[mask ^ bit]);
            }
            best[mask] = (byte) (bestWithoutOne + (sum[mask] == 0 ? 1 : 0));
        }

        int[] order = restoreOrder(full, n, sum, best);
        return settleGroups(order, people, amounts);
    }

    /**
     * Восстанавливает порядок участников, в котором префиксы с нулевой суммой
     * встречаются {@code best[full]} раз — это и есть границы групп.
     */
    private static int[] restoreOrder(int full, int n, long[] sum, byte[] best) {
        int[] order = new int[n];
        int position = n - 1;
        int mask = full;
        while (mask != 0) {
            int target = best[mask] - (sum[mask] == 0 ? 1 : 0);
            int chosen = -1;
            for (int rest = mask; rest != 0; rest &= rest - 1) {
                int bit = rest & -rest;
                if (best[mask ^ bit] == target) {
                    chosen = Integer.numberOfTrailingZeros(bit);
                    break;
                }
            }
            order[position--] = chosen;
            mask ^= 1 << chosen;
        }
        return order;
    }

    /** Режет порядок на группы с нулевой суммой и закрывает каждую жадным алгоритмом. */
    private <P> List<Transfer<P>> settleGroups(int[] order, List<P> people, long[] amounts) {
        List<Transfer<P>> transfers = new ArrayList<>();
        Map<P, Money> group = new LinkedHashMap<>();
        long running = 0;
        for (int index : order) {
            group.put(people.get(index), Money.ofKopecks(amounts[index]));
            running += amounts[index];
            if (running == 0) {
                transfers.addAll(greedy.simplify(group));
                group = new LinkedHashMap<>();
            }
        }
        return List.copyOf(transfers);
    }
}
