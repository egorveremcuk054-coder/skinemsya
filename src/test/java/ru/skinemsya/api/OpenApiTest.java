package ru.skinemsya.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiTest extends ApiTestSupport {

    @Test
    @DisplayName("документация API доступна без токена")
    void apiDocsArePublic() throws Exception {
        get("/v3/api-docs", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Скинемся API"))
                .andExpect(jsonPath("$.paths['/api/groups/{groupId}/settle-up']").exists());
    }

    @Test
    @DisplayName("health-check доступен без токена")
    void healthIsPublic() throws Exception {
        get("/actuator/health", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
