package ru.skinemsya.balance;

import ru.skinemsya.group.MemberRef;

import java.math.BigDecimal;

/**
 * Рекомендуемый перевод: {@code from} переводит {@code amount} рублей участнику {@code to}.
 */
public record TransferResponse(MemberRef from, MemberRef to, BigDecimal amount) {
}
