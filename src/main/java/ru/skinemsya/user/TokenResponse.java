package ru.skinemsya.user;

/**
 * @param accessToken JWT для заголовка {@code Authorization: Bearer <token>}
 * @param tokenType   всегда {@code Bearer}
 * @param expiresIn   через сколько секунд токен истечёт
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
