package ru.skinemsya.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий из README: поездка в Питер.
 */
class TripScenarioApiTest extends ApiTestSupport {

    @Test
    @DisplayName("поездка в Питер: траты → балансы → один перевод → все в расчёте")
    void tripToSaintPetersburg() throws Exception {
        String token = signUp("Аня");
        String group = createGroup(token, "Поездка в Питер");
        long groupId = id(group, "$.id");
        long anya = id(group, "$.members[0].id");
        long boris = addMember(token, groupId, "Борис");
        long vika = addMember(token, groupId, "Вика");
        String base = "/api/groups/" + groupId;

        // Аня оплатила жильё на всех (список участников не указан — значит, все).
        post(base + "/expenses", token, """
                {"description": "Жильё", "amount": 6000, "payerId": %d, "split": {"type": "EQUAL"}}
                """.formatted(anya))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares.length()").value(3));

        // Борис оплатил бензин на троих.
        post(base + "/expenses", token, """
                {"description": "Бензин", "amount": 3000, "payerId": %d,
                 "split": {"type": "EQUAL", "memberIds": [%d, %d, %d]}}
                """.formatted(boris, anya, boris, vika))
                .andExpect(status().isCreated());

        String balances = body(get(base + "/balances", token).andExpect(status().isOk()));
        assertThat(decimal(balances, "$[0].balance")).isEqualByComparingTo("3000");
        assertThat(decimal(balances, "$[1].balance")).isEqualByComparingTo("0");
        assertThat(decimal(balances, "$[2].balance")).isEqualByComparingTo("-3000");

        String transfers = body(get(base + "/settle-up", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].from.name").value("Вика"))
                .andExpect(jsonPath("$[0].to.name").value("Аня")));
        assertThat(decimal(transfers, "$[0].amount")).isEqualByComparingTo("3000");

        // Вика вернула долг.
        post(base + "/settlements", token, """
                {"fromMemberId": %d, "toMemberId": %d, "amount": 3000}
                """.formatted(vika, anya))
                .andExpect(status().isCreated());

        String after = body(get(base + "/balances", token).andExpect(status().isOk()));
        for (int i = 0; i < 3; i++) {
            assertThat(decimal(after, "$[%d].balance".formatted(i))).isEqualByComparingTo("0");
        }
        get(base + "/settle-up", token).andExpect(jsonPath("$.length()").value(0));
        get(base + "/expenses", token).andExpect(jsonPath("$.length()").value(2));
        get(base + "/settlements", token).andExpect(jsonPath("$.length()").value(1));
    }
}
