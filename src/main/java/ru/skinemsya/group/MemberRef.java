package ru.skinemsya.group;

/**
 * Краткая ссылка на участника в ответах API.
 */
public record MemberRef(Long id, String name) {

    public static MemberRef from(GroupMember member) {
        return new MemberRef(member.getId(), member.getName());
    }
}
