package ru.skinemsya.settlement;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Участник {@code fromMemberId} перевёл {@code amount} рублей участнику {@code toMemberId}.
 */
public record CreateSettlementRequest(
        @NotNull(message = "Укажите, кто переводит")
        Long fromMemberId,

        @NotNull(message = "Укажите, кому переводят")
        Long toMemberId,

        @NotNull(message = "Укажите сумму")
        @Positive(message = "Сумма должна быть положительной")
        @Digits(integer = 12, fraction = 2, message = "Сумма — не больше 2 знаков после запятой")
        BigDecimal amount
) {
}
