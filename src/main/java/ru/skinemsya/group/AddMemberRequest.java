package ru.skinemsya.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Новый участник группы.
 *
 * @param name  имя участника; можно не указывать, если указан email — тогда возьмём имя пользователя
 * @param email email зарегистрированного пользователя, если нужно дать ему доступ к группе
 */
public record AddMemberRequest(
        @Size(max = 100, message = "Имя должно быть не длиннее 100 символов")
        String name,

        @Email(message = "Некорректный email")
        String email
) {
}
