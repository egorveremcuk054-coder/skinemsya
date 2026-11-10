package ru.skinemsya.group;

import java.time.Instant;
import java.util.List;

public record GroupResponse(Long id, String name, Long ownerId, Instant createdAt, List<MemberResponse> members) {

    public static GroupResponse from(ExpenseGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getOwnerId(),
                group.getCreatedAt(),
                group.getMembers().stream().map(MemberResponse::from).toList());
    }
}
