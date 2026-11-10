package ru.skinemsya.settlement;

import ru.skinemsya.core.Money;
import ru.skinemsya.group.MemberRef;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(Long id, MemberRef from, MemberRef to, BigDecimal amount, Instant createdAt) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                MemberRef.from(settlement.getFromMember()),
                MemberRef.from(settlement.getToMember()),
                Money.ofKopecks(settlement.getAmount()).toBigDecimal(),
                settlement.getCreatedAt());
    }
}
