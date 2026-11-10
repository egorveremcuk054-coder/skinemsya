package ru.skinemsya.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Достаёт id текущего пользователя из JWT: он лежит в поле {@code sub}.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
