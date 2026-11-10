package ru.skinemsya.balance;

import ru.skinemsya.group.MemberRef;

import java.math.BigDecimal;

/**
 * @param balance больше нуля — участнику должны, меньше нуля — должен он
 */
public record BalanceResponse(MemberRef member, BigDecimal balance) {
}
