package ru.skinemsya.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Настройки JWT из {@code application.yaml}.
 *
 * @param secret секрет для подписи токенов в Base64, минимум 32 байта
 * @param ttl    время жизни токена
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
