package ru.skinemsya.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Новая трата.
 *
 * @param amount  сумма в рублях, например {@code 1500.50}
 * @param spentOn дата траты; по умолчанию — сегодня
 */
public record CreateExpenseRequest(
        @NotBlank(message = "Укажите, на что потратили")
        @Size(max = 200, message = "Описание должно быть не длиннее 200 символов")
        String description,

        @NotNull(message = "Укажите сумму")
        @Positive(message = "Сумма должна быть положительной")
        @Digits(integer = 12, fraction = 2, message = "Сумма — не больше 2 знаков после запятой")
        BigDecimal amount,

        @NotNull(message = "Укажите, кто заплатил")
        Long payerId,

        LocalDate spentOn,

        @NotNull(message = "Укажите, как разделить трату")
        @Valid
        SplitRequest split
) {
}
