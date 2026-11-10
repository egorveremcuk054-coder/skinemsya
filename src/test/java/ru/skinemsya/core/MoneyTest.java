package ru.skinemsya.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("складывает и вычитает суммы")
    void arithmetic() {
        Money hundred = Money.ofRubles(100);
        Money fifty = Money.ofRubles(50);

        assertThat(hundred.plus(fifty)).isEqualTo(Money.ofRubles(150));
        assertThat(fifty.minus(hundred)).isEqualTo(Money.ofRubles(-50));
        assertThat(fifty.negate()).isEqualTo(Money.ofRubles(-50));
        assertThat(Money.ofRubles(-50).abs()).isEqualTo(fifty);
    }

    @Test
    @DisplayName("0.1 + 0.2 ровно 0.3, в отличие от double")
    void noFloatingPointErrors() {
        Money sum = Money.of(new BigDecimal("0.10")).plus(Money.of(new BigDecimal("0.20")));

        assertThat(sum).isEqualTo(Money.of(new BigDecimal("0.30")));
    }

    @Test
    @DisplayName("создаётся из рублей с копейками")
    void fromBigDecimal() {
        assertThat(Money.of(new BigDecimal("1500.5")).kopecks()).isEqualTo(150_050);
        assertThat(Money.of(new BigDecimal("1500.50")).toBigDecimal()).isEqualByComparingTo("1500.50");
    }

    @Test
    @DisplayName("не принимает доли копеек")
    void rejectsFractionsOfKopeck() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.555")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1000 ₽ на троих делятся без потери копейки")
    void splitEvenlyKeepsEveryKopeck() {
        List<Money> parts = Money.ofRubles(1000).splitEvenly(3);

        assertThat(parts).containsExactly(
                Money.ofKopecks(33_334), Money.ofKopecks(33_333), Money.ofKopecks(33_333));
        assertThat(Money.sum(parts)).isEqualTo(Money.ofRubles(1000));
    }

    @Test
    @DisplayName("отрицательная сумма тоже делится без потерь")
    void splitEvenlyNegative() {
        List<Money> parts = Money.ofKopecks(-100).splitEvenly(3);

        assertThat(parts).containsExactly(Money.ofKopecks(-34), Money.ofKopecks(-33), Money.ofKopecks(-33));
    }

    @Test
    @DisplayName("нельзя разделить на ноль частей")
    void splitIntoZeroParts() {
        assertThatThrownBy(() -> Money.ofRubles(100).splitEvenly(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("делит пропорционально весам методом наибольших остатков")
    void allocateByWeights() {
        List<Money> parts = Money.ofRubles(100).allocate(List.of(1, 2));

        assertThat(parts).containsExactly(Money.ofKopecks(3_333), Money.ofKopecks(6_667));
    }

    @Test
    @DisplayName("сумма частей при делении по весам всегда равна исходной")
    void allocatePreservesTotal() {
        Money total = Money.ofKopecks(1_000_001);

        List<Money> parts = total.allocate(List.of(3, 7, 11, 13));

        assertThat(Money.sum(parts)).isEqualTo(total);
    }

    @ParameterizedTest(name = "{0} копеек → \"{1}\"")
    @CsvSource({
            "150, '1,50 ₽'",
            "-150, '-1,50 ₽'",
            "0, '0,00 ₽'",
            "100000, '1000,00 ₽'",
            "5, '0,05 ₽'"
    })
    @DisplayName("форматируется по-русски")
    void formatting(long kopecks, String expected) {
        assertThat(Money.ofKopecks(kopecks)).hasToString(expected);
    }

    @Test
    @DisplayName("сравнивается по величине")
    void comparison() {
        assertThat(Money.ofRubles(1)).isLessThan(Money.ofRubles(2));
        assertThat(Money.min(Money.ofRubles(5), Money.ofRubles(3))).isEqualTo(Money.ofRubles(3));
    }

    @Test
    @DisplayName("ловит переполнение вместо тихой ошибки")
    void overflow() {
        assertThatThrownBy(() -> Money.ofKopecks(Long.MAX_VALUE).plus(Money.ofKopecks(1)))
                .isInstanceOf(ArithmeticException.class);
    }
}
