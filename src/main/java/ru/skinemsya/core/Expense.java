package ru.skinemsya.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Трата: кто заплатил, сколько и как сумма делится между участниками.
 *
 * @param payer  кто заплатил
 * @param amount сколько заплатил
 * @param shares доля каждого участника; в сумме равны {@code amount}
 * @param <P>    идентификатор участника
 */
public record Expense<P>(P payer, Money amount, Map<P, Money> shares) {

    public Expense {
        Objects.requireNonNull(payer, "payer");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(shares, "shares");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Сумма траты должна быть положительной");
        }
        Money sum = Money.sum(shares.values());
        if (!sum.equals(amount)) {
            throw new IllegalArgumentException(
                    "Сумма долей (%s) не совпадает с суммой траты (%s)".formatted(sum, amount));
        }
        shares = Collections.unmodifiableMap(new LinkedHashMap<>(shares));
    }

    public static <P> Expense<P> of(P payer, Money amount, Split<P> split) {
        return new Expense<>(payer, amount, split.apply(amount));
    }
}
