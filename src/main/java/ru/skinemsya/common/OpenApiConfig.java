package ru.skinemsya.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Документация API: Swagger UI на {@code /swagger-ui.html}.
 * Кнопка «Authorize» принимает токен из {@code POST /api/auth/login}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Скинемся API",
                version = "v1",
                description = "Делёж общих расходов: группы, траты, балансы и минимальный набор переводов"),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
