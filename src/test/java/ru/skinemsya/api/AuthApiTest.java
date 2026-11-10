package ru.skinemsya.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    @DisplayName("регистрация: email приводится к нижнему регистру, пароль не возвращается")
    void register() throws Exception {
        String email = uniqueEmail();

        post("/api/auth/register", null, """
                {"email": "%s", "password": "password123", "displayName": "Аня"}
                """.formatted(email.toUpperCase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Аня"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("нельзя зарегистрироваться дважды с одним email")
    void duplicateEmail() throws Exception {
        String email = uniqueEmail();
        register(email, "Аня");

        post("/api/auth/register", null, """
                {"email": "%s", "password": "password123", "displayName": "Аня 2"}
                """.formatted(email))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("невалидные данные: 400 и список ошибок по полям")
    void validation() throws Exception {
        post("/api/auth/register", null, """
                {"email": "not-an-email", "password": "123", "displayName": ""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    @DisplayName("вход с неверным паролем: 401")
    void wrongPassword() throws Exception {
        String email = uniqueEmail();
        register(email, "Аня");

        post("/api/auth/login", null, """
                {"email": "%s", "password": "wrong-password"}
                """.formatted(email))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("по токену можно узнать текущего пользователя")
    void me() throws Exception {
        String token = signUp("Борис");

        get("/api/users/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Борис"));
    }

    @Test
    @DisplayName("гость входит без регистрации и может создать группу")
    void guest() throws Exception {
        String json = body(post("/api/auth/guest", null, "{}").andExpect(status().isCreated()));
        String token = JsonPath.read(json, "$.accessToken");

        get("/api/users/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Я"))
                .andExpect(jsonPath("$.email").doesNotExist());

        post("/api/groups", token, """
                {"name": "Поездка"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[0].name").value("Я"));
    }

    @Test
    @DisplayName("гость может сам выбрать имя, а запрос без тела тоже работает")
    void guestWithName() throws Exception {
        post("/api/auth/guest", null, """
                {"displayName": "Аня"}
                """).andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/auth/guest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("без токена или с поддельным токеном: 401")
    void unauthorized() throws Exception {
        get("/api/users/me", null).andExpect(status().isUnauthorized());
        get("/api/users/me", "not.a.token").andExpect(status().isUnauthorized());
    }
}
