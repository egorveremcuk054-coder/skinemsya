package ru.skinemsya.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    private final GreedyDebtSimplifier greedy = new GreedyDebtSimplifier();
    private final OptimalDebtSimplifier optimal = new OptimalDebtSimplifier();
    private final AdaptiveDebtSimplifier adaptive = new AdaptiveDebtSimplifier();

    @Test
    @DisplayName("поездка в Питер закрывается одним переводом")
    void tripNeedsOneTransfer() {
        Map<String, Money> balances = balances(
                "Аня", 3000_00,
                "Борис", 0,
                "Вика", -3000_00);

        for (DebtSimplifier simplifier : List.of(greedy, optimal, adaptive)) {
            assertThat(simplifier.simplify(balances))
                    .containsExactly(new Transfer<>("Вика", "Аня", Money.ofRubles(3000)));
        }
    }

    @Test
    @DisplayName("пример, где жадный алгоритм делает лишний перевод, а точный — нет")
    void optimalBeatsGreedy() {
        // Группы {A, C, D} и {B, E} закрываются по отдельности: 2 + 1 = 3 перевода.
        // Жадный алгоритм смешивает их и тратит 4 перевода.
        Map<String, Money> balances = balances(
                "A", 5_00,
                "B", 4_00,
                "C", -2_00,
                "D", -3_00,
                "E", -4_00);

        List<Transfer<String>> greedyResult = greedy.simplify(balances);
        List<Transfer<String>> optimalResult = optimal.simplify(balances);

        assertThat(greedyResult).hasSize(4);
        assertThat(optimalResult).hasSize(3);
        assertSettles(balances, greedyResult);
        assertSettles(balances, optimalResult);
    }

    @Test
    @DisplayName("на случайных данных: все в расчёте, точный ≤ жадного ≤ n − 1")
    void randomBalances() {
        Random random = new Random(42);
        for (int round = 0; round < 300; round++) {
            Map<String, Money> balances = randomBalances(random, 2 + random.nextInt(9));
            long nonZero = balances.values().stream().filter(b -> !b.isZero()).count();

            List<Transfer<String>> greedyResult = greedy.simplify(balances);
            List<Transfer<String>> optimalResult = optimal.simplify(balances);

            assertSettles(balances, greedyResult);
            assertSettles(balances, optimalResult);
            assertThat(optimalResult.size()).isLessThanOrEqualTo(greedyResult.size());
            assertThat((long) greedyResult.size()).isLessThanOrEqualTo(Math.max(0, nonZero - 1));
        }
    }

    @Test
    @DisplayName("если все в расчёте, переводов нет")
    void nothingToSettle() {
        Map<String, Money> balances = balances("Аня", 0, "Борис", 0);

        assertThat(greedy.simplify(balances)).isEmpty();
        assertThat(optimal.simplify(balances)).isEmpty();
    }

    @Test
    @DisplayName("не принимает балансы с ненулевой суммой")
    void rejectsUnbalanced() {
        Map<String, Money> balances = balances("Аня", 100, "Борис", -99);

        assertThatThrownBy(() -> greedy.simplify(balances)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> optimal.simplify(balances)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("адаптивный алгоритм справляется с большой группой")
    void adaptiveHandlesLargeGroups() {
        Map<String, Money> balances = randomBalances(new Random(7), 50);

        assertSettles(balances, adaptive.simplify(balances));
    }

    private static Map<String, Money> balances(Object... nameAndKopecks) {
        Map<String, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < nameAndKopecks.length; i += 2) {
            result.put((String) nameAndKopecks[i], Money.ofKopecks(((Number) nameAndKopecks[i + 1]).longValue()));
        }
        return result;
    }

    /** Случайные балансы с нулевой суммой; суммы небольшие, чтобы чаще возникали группы с нулём. */
    private static Map<String, Money> randomBalances(Random random, int people) {
        Map<String, Money> result = new LinkedHashMap<>();
        long total = 0;
        for (int i = 0; i < people - 1; i++) {
            long kopecks = (random.nextInt(21) - 10) * 100L;
            result.put("p" + i, Money.ofKopecks(kopecks));
            total += kopecks;
        }
        result.put("p" + (people - 1), Money.ofKopecks(-total));
        return result;
    }

    private static void assertSettles(Map<String, Money> balances, List<Transfer<String>> transfers) {
        Map<String, Money> remaining = new HashMap<>(balances);
        for (Transfer<String> transfer : transfers) {
            remaining.merge(transfer.from(), transfer.amount(), Money::plus);
            remaining.merge(transfer.to(), transfer.amount().negate(), Money::plus);
        }
        assertThat(remaining.values()).allMatch(Money::isZero);
    }
}
