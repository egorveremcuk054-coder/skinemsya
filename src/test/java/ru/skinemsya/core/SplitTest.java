package ru.skinemsya.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class SplitTest {

    @Test
    @DisplayName("поровну: лишняя копейка достаётся первому")
    void equalSplit() {
        Split<String> split = new Split.Equal<>(List.of("Аня", "Борис", "Вика"));

        Map<String, Money> shares = split.apply(Money.ofRubles(100));

        assertThat(shares).containsExactly(
                entry("Аня", Money.ofKopecks(3_334)),
                entry("Борис", Money.ofKopecks(3_333)),
                entry("Вика", Money.ofKopecks(3_333)));
    }

    @Test
    @DisplayName("поровну: участники не должны повторяться")
    void equalSplitRejectsDuplicates() {
        assertThatThrownBy(() -> new Split.Equal<>(List.of("Аня", "Аня")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("точные суммы должны сходиться с суммой траты")
    void exactSplit() {
        Map<String, Money> amounts = new LinkedHashMap<>();
        amounts.put("Аня", Money.ofRubles(700));
        amounts.put("Борис", Money.ofRubles(300));
        Split<String> split = new Split.Exact<>(amounts);

        assertThat(split.apply(Money.ofRubles(1000))).isEqualTo(amounts);
        assertThatThrownBy(() -> split.apply(Money.ofRubles(999)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не совпадает");
    }

    @Test
    @DisplayName("по долям: 2:1:1")
    void sharesSplit() {
        Map<String, Integer> shares = new LinkedHashMap<>();
        shares.put("Аня", 2);
        shares.put("Борис", 1);
        shares.put("Вика", 1);

        Map<String, Money> result = new Split.Shares<>(shares).apply(Money.ofRubles(1000));

        assertThat(result).containsExactly(
                entry("Аня", Money.ofRubles(500)),
                entry("Борис", Money.ofRubles(250)),
                entry("Вика", Money.ofRubles(250)));
    }

    @Test
    @DisplayName("по долям: доли должны быть положительными")
    void sharesMustBePositive() {
        assertThatThrownBy(() -> new Split.Shares<>(Map.of("Аня", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("трата проверяет, что доли сходятся с суммой")
    void expenseValidatesShares() {
        assertThatThrownBy(() -> new Expense<>("Аня", Money.ofRubles(100), Map.of("Аня", Money.ofRubles(99))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
