package ru.skinemsya.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.skinemsya.TestcontainersConfiguration;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Основа для API-тестов: поднимает приложение целиком с настоящим PostgreSQL
 * и даёт короткие методы для типичных запросов.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions post(String url, String token, String json) throws Exception {
        return mvc.perform(withToken(MockMvcRequestBuilders.post(url), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(withToken(MockMvcRequestBuilders.get(url), token));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(withToken(MockMvcRequestBuilders.delete(url), token));
    }

    /** Регистрирует пользователя с уникальным email и возвращает его токен. */
    protected String signUp(String displayName) throws Exception {
        String email = uniqueEmail();
        register(email, displayName);
        return login(email);
    }

    protected void register(String email, String displayName) throws Exception {
        post("/api/auth/register", null, """
                {"email": "%s", "password": "password123", "displayName": "%s"}
                """.formatted(email, displayName))
                .andExpect(status().isCreated());
    }

    protected String login(String email) throws Exception {
        String json = body(post("/api/auth/login", null, """
                {"email": "%s", "password": "password123"}
                """.formatted(email))
                .andExpect(status().isOk()));
        return JsonPath.read(json, "$.accessToken");
    }

    /** Создаёт группу и возвращает её JSON. */
    protected String createGroup(String token, String name) throws Exception {
        return body(post("/api/groups", token, """
                {"name": "%s"}
                """.formatted(name))
                .andExpect(status().isCreated()));
    }

    protected long addMember(String token, long groupId, String name) throws Exception {
        String json = body(post("/api/groups/%d/members".formatted(groupId), token, """
                {"name": "%s"}
                """.formatted(name))
                .andExpect(status().isCreated()));
        return id(json, "$.id");
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    protected static BigDecimal decimal(String json, String path) {
        return new BigDecimal(JsonPath.read(json, path).toString());
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
