package ru.skinemsya.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Укажите email")
        @Email(message = "Некорректный email")
        @Size(max = 255, message = "Слишком длинный email")
        String email,

        @NotBlank(message = "Укажите пароль")
        @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
        String password,

        @NotBlank(message = "Укажите имя")
        @Size(max = 100, message = "Имя должно быть не длиннее 100 символов")
        String displayName
) {
}
