package ru.skinemsya.expense;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Доля одного участника.
 *
 * @param memberId id участника
 * @param amount   точная сумма — для {@link SplitType#EXACT}
 * @param share    вес доли — для {@link SplitType#SHARES}
 */
public record SplitPartRequest(
        @NotNull(message = "Укажите участника")
        Long memberId,

        @PositiveOrZero(message = "Сумма не может быть отрицательной")
        @Digits(integer = 12, fraction = 2, message = "Сумма — не больше 2 знаков после запятой")
        BigDecimal amount,

        @Positive(message = "Доля должна быть положительной")
        Integer share
) {
}
