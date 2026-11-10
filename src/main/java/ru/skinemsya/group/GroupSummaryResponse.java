package ru.skinemsya.group;

import java.time.Instant;

public record GroupSummaryResponse(Long id, String name, Instant createdAt) {

    public static GroupSummaryResponse from(ExpenseGroup group) {
        return new GroupSummaryResponse(group.getId(), group.getName(), group.getCreatedAt());
    }
}
