package ru.skinemsya.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseApiTest extends ApiTestSupport {

    private String token;
    private String base;
    private long anya;
    private long boris;
    private long vika;

    @BeforeEach
    void setUp() throws Exception {
        token = signUp("Аня");
        String group = createGroup(token, "Квартира");
        long groupId = id(group, "$.id");
        anya = id(group, "$.members[0].id");
        boris = addMember(token, groupId, "Борис");
        vika = addMember(token, groupId, "Вика");
        base = "/api/groups/" + groupId;
    }

    @Test
    @DisplayName("деление по долям 2:1:1")
    void sharesSplit() throws Exception {
        String json = body(post(base + "/expenses", token, """
                {"description": "Продукты", "amount": 1000, "payerId": %d,
                 "split": {"type": "SHARES", "parts": [
                    {"memberId": %d, "share": 2}, {"memberId": %d, "share": 1}, {"memberId": %d, "share": 1}]}}
                """.formatted(anya, anya, boris, vika))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splitType").value("SHARES")));

        assertThat(decimal(json, "$.shares[0].amount")).isEqualByComparingTo("500");
        assertThat(decimal(json, "$.shares[1].amount")).isEqualByComparingTo("250");
        assertThat(decimal(json, "$.shares[2].amount")).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("100 ₽ на троих: копейка не теряется")
    void equalSplitKeepsKopecks() throws Exception {
        String json = body(post(base + "/expenses", token, """
                {"description": "Кофе", "amount": 100, "payerId": %d, "split": {"type": "EQUAL"}}
                """.formatted(anya))
                .andExpect(status().isCreated()));

        assertThat(decimal(json, "$.shares[0].amount")).isEqualByComparingTo("33.34");
        assertThat(decimal(json, "$.shares[1].amount")).isEqualByComparingTo("33.33");
        assertThat(decimal(json, "$.shares[2].amount")).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("точные суммы, которые не сходятся с общей суммой: 400")
    void exactSplitMismatch() throws Exception {
        post(base + "/expenses", token, """
                {"description": "Такси", "amount": 1000, "payerId": %d,
                 "split": {"type": "EXACT", "parts": [{"memberId": %d, "amount": 600}, {"memberId": %d, "amount": 300}]}}
                """.formatted(anya, anya, boris))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("не совпадает")));
    }

    @Test
    @DisplayName("участник не из группы: 400")
    void unknownMember() throws Exception {
        post(base + "/expenses", token, """
                {"description": "Такси", "amount": 1000, "payerId": 999999, "split": {"type": "EQUAL"}}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("не состоит в группе")));
    }

    @Test
    @DisplayName("доли копейки в сумме: 400")
    void tooManyDecimals() throws Exception {
        post(base + "/expenses", token, """
                {"description": "Такси", "amount": 10.555, "payerId": %d, "split": {"type": "EQUAL"}}
                """.formatted(anya))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("трату можно удалить")
    void deleteExpense() throws Exception {
        String json = body(post(base + "/expenses", token, """
                {"description": "Пицца", "amount": 900, "payerId": %d, "split": {"type": "EQUAL"}}
                """.formatted(boris))
                .andExpect(status().isCreated()));

        delete(base + "/expenses/" + id(json, "$.id"), token).andExpect(status().isNoContent());

        get(base + "/expenses", token).andExpect(jsonPath("$.length()").value(0));
    }
}
