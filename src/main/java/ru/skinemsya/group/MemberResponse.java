package ru.skinemsya.group;

/**
 * @param userId id пользователя, если участник зарегистрирован в приложении, иначе {@code null}
 */
public record MemberResponse(Long id, String name, Long userId) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(member.getId(), member.getName(), member.getUserId());
    }
}
