package ru.skinemsya.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Как разделить трату.
 * <ul>
 *     <li>{@code EQUAL} — поровну между {@code memberIds}; если список пуст — между всеми участниками группы;</li>
 *     <li>{@code EXACT} — точными суммами из {@code parts[].amount};</li>
 *     <li>{@code SHARES} — пропорционально {@code parts[].share}.</li>
 * </ul>
 */
public record SplitRequest(
        @NotNull(message = "Укажите способ деления")
        SplitType type,

        List<@NotNull Long> memberIds,

        List<@Valid SplitPartRequest> parts
) {
}
