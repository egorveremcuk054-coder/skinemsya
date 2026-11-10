package ru.skinemsya.user;

import jakarta.validation.constraints.Size;

/**
 * @param displayName как назвать гостя в группах; по умолчанию «Я»
 */
public record GuestRequest(
        @Size(max = 100, message = "Имя должно быть не длиннее 100 символов")
        String displayName
) {
}
