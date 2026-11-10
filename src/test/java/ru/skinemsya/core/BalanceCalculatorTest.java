package ru.skinemsya.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class BalanceCalculatorTest {

    private static final List<String> TRIP = List.of("Аня", "Борис", "Вика");

    @Test
    @DisplayName("поездка в Питер: Аня +3000, Борис 0, Вика −3000")
    void tripToSaintPetersburg() {
        List<Expense<String>> expenses = List.of(
                Expense.of("Аня", Money.ofRubles(6000), new Split.Equal<>(TRIP)),
                Expense.of("Борис", Money.ofRubles(3000), new Split.Equal<>(TRIP)));

        Map<String, Money> balances = BalanceCalculator.calculate(TRIP, expenses, List.of());

        assertThat(balances).containsExactly(
                entry("Аня", Money.ofRubles(3000)),
                entry("Борис", Money.ZERO),
                entry("Вика", Money.ofRubles(-3000)));
    }

    @Test
    @DisplayName("после перевода долг закрывается")
    void settlementClosesDebt() {
        List<Expense<String>> expenses = List.of(
                Expense.of("Аня", Money.ofRubles(6000), new Split.Equal<>(TRIP)),
                Expense.of("Борис", Money.ofRubles(3000), new Split.Equal<>(TRIP)));
        List<Transfer<String>> settlements = List.of(new Transfer<>("Вика", "Аня", Money.ofRubles(3000)));

        Map<String, Money> balances = BalanceCalculator.calculate(TRIP, expenses, settlements);

        assertThat(balances.values()).allMatch(Money::isZero);
    }

    @Test
    @DisplayName("сумма балансов всегда равна нулю")
    void balancesSumToZero() {
        List<Expense<String>> expenses = List.of(
                Expense.of("Аня", Money.ofKopecks(100_001), new Split.Equal<>(TRIP)),
                Expense.of("Вика", Money.ofKopecks(77_777), new Split.Equal<>(List.of("Аня", "Вика"))));

        Map<String, Money> balances = BalanceCalculator.calculate(TRIP, expenses, List.of());

        assertThat(Money.sum(balances.values())).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("участник без трат попадает в результат с нулём")
    void participantWithoutExpenses() {
        Map<String, Money> balances = BalanceCalculator.calculate(TRIP, List.of(), List.of());

        assertThat(balances).hasSize(3).allSatisfy((name, balance) -> assertThat(balance).isEqualTo(Money.ZERO));
    }
}
