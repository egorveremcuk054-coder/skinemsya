package ru.skinemsya.core;

import java.util.Objects;

/**
 * Перевод денег от одного участника другому.
 *
 * @param <P> идентификатор участника
 */
public record Transfer<P>(P from, P to, Money amount) {

    public Transfer {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (from.equals(to)) {
            throw new IllegalArgumentException("Нельзя перевести деньги самому себе");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной");
        }
    }
}
