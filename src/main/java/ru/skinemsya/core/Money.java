package ru.skinemsya.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Денежная сумма в рублях.
 * <p>
 * Хранится в копейках в {@code long}: арифметика с {@code double} теряет точность
 * ({@code 0.1 + 0.2 != 0.3}), а для денег это недопустимо.
 * Все операции проверяют переполнение и бросают {@link ArithmeticException}.
 */
public record Money(long kopecks) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    private static final int KOPECKS_IN_RUBLE = 100;

    public static Money ofKopecks(long kopecks) {
        return new Money(kopecks);
    }

    public static Money ofRubles(long rubles) {
        return new Money(Math.multiplyExact(rubles, KOPECKS_IN_RUBLE));
    }

    /**
     * Создаёт сумму из рублей с копейками, например {@code 1500.50}.
     *
     * @throws IllegalArgumentException если в сумме больше двух знаков после запятой
     */
    public static Money of(BigDecimal rubles) {
        try {
            long kopecks = rubles.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
            return new Money(kopecks);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Сумма должна быть указана с точностью до копейки: " + rubles, e);
        }
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(kopecks, 2);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(kopecks, other.kopecks));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(kopecks, other.kopecks));
    }

    public Money negate() {
        return new Money(Math.negateExact(kopecks));
    }

    public Money abs() {
        return isNegative() ? negate() : this;
    }

    public boolean isZero() {
        return kopecks == 0;
    }

    public boolean isPositive() {
        return kopecks > 0;
    }

    public boolean isNegative() {
        return kopecks < 0;
    }

    public static Money min(Money a, Money b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static Money sum(Iterable<Money> amounts) {
        Money total = ZERO;
        for (Money amount : amounts) {
            total = total.plus(amount);
        }
        return total;
    }

    /**
     * Делит сумму на {@code parts} равных частей без потери копеек.
     * Остаток раздаётся по одной копейке первым частям:
     * 1000 ₽ на троих → [333,34 ₽, 333,33 ₽, 333,33 ₽].
     */
    public List<Money> splitEvenly(int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("Количество частей должно быть положительным: " + parts);
        }
        long base = kopecks / parts;
        long remainder = kopecks % parts;
        long extra = Long.signum(remainder);

        List<Money> result = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            result.add(new Money(i < Math.abs(remainder) ? base + extra : base));
        }
        return List.copyOf(result);
    }

    /**
     * Делит сумму пропорционально весам методом наибольших остатков.
     * Например, 100 ₽ в пропорции 1:2 → [33,33 ₽, 66,67 ₽].
     * Сумма частей всегда точно равна исходной сумме.
     */
    public List<Money> allocate(List<Integer> weights) {
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Нужен хотя бы один вес");
        }
        if (weights.stream().anyMatch(w -> w == null || w < 0)) {
            throw new IllegalArgumentException("Веса не могут быть отрицательными: " + weights);
        }
        long totalWeight = weights.stream().mapToLong(Integer::longValue).sum();
        if (totalWeight == 0) {
            throw new IllegalArgumentException("Сумма весов должна быть положительной");
        }

        int n = weights.size();
        long[] parts = new long[n];
        long[] remainders = new long[n];
        long distributed = 0;
        for (int i = 0; i < n; i++) {
            long scaled = Math.multiplyExact(kopecks, (long) weights.get(i));
            parts[i] = Math.floorDiv(scaled, totalWeight);
            remainders[i] = Math.floorMod(scaled, totalWeight);
            distributed += parts[i];
        }

        long leftover = kopecks - distributed;
        IntStream.range(0, n)
                .boxed()
                .sorted(Comparator.comparingLong((Integer i) -> remainders[i]).reversed())
                .limit(leftover)
                .forEach(i -> parts[i]++);

        return IntStream.range(0, n).mapToObj(i -> new Money(parts[i])).toList();
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(kopecks, other.kopecks);
    }

    /** Форматирует сумму как {@code "1500,50 ₽"} или {@code "-1,50 ₽"}. */
    @Override
    public String toString() {
        long abs = Math.abs(kopecks);
        String sign = kopecks < 0 ? "-" : "";
        return "%s%d,%02d ₽".formatted(sign, abs / KOPECKS_IN_RUBLE, abs % KOPECKS_IN_RUBLE);
    }
}
