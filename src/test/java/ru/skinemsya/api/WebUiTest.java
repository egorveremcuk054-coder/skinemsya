package ru.skinemsya.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebUiTest extends ApiTestSupport {

    @Test
    @DisplayName("главная страница открывается без токена")
    void indexIsPublic() throws Exception {
        get("/", null)
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        String html = body(get("/index.html", null).andExpect(status().isOk()));
        assertThat(html).contains("<title>Скинемся</title>", "app.js");
    }

    @Test
    @DisplayName("скрипты и стили отдаются без токена")
    void assetsArePublic() throws Exception {
        get("/app.js", null).andExpect(status().isOk());
        get("/app.css", null).andExpect(status().isOk());
    }
}
