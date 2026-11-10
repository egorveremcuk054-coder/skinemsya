package ru.skinemsya.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupAccessApiTest extends ApiTestSupport {

    @Test
    @DisplayName("чужая группа выглядит как несуществующая: 404")
    void strangerCannotSeeGroup() throws Exception {
        String owner = signUp("Аня");
        String stranger = signUp("Незнакомец");
        long groupId = id(createGroup(owner, "Секретная группа"), "$.id");

        get("/api/groups/" + groupId, stranger).andExpect(status().isNotFound());
        get("/api/groups/" + groupId + "/balances", stranger).andExpect(status().isNotFound());
        post("/api/groups/" + groupId + "/members", stranger, """
                {"name": "Я"}
                """).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("пользователя можно добавить по email, и он увидит группу")
    void addRegisteredUserByEmail() throws Exception {
        String owner = signUp("Аня");
        String borisEmail = uniqueEmail();
        register(borisEmail, "Борис");
        String borisToken = login(borisEmail);
        long groupId = id(createGroup(owner, "Дача"), "$.id");

        post("/api/groups/" + groupId + "/members", owner, """
                {"email": "%s"}
                """.formatted(borisEmail))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Борис"))
                .andExpect(jsonPath("$.userId").isNumber());

        get("/api/groups", borisToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(groupId));
    }

    @Test
    @DisplayName("имена участников в группе не повторяются")
    void duplicateMemberName() throws Exception {
        String owner = signUp("Аня");
        long groupId = id(createGroup(owner, "Дача"), "$.id");
        addMember(owner, groupId, "Борис");

        post("/api/groups/" + groupId + "/members", owner, """
                {"name": "борис"}
                """).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("удалить группу может только создатель")
    void onlyOwnerCanDeleteGroup() throws Exception {
        String owner = signUp("Аня");
        String memberEmail = uniqueEmail();
        register(memberEmail, "Борис");
        String memberToken = login(memberEmail);
        String group = createGroup(owner, "Поход");
        long groupId = id(group, "$.id");
        long anya = id(group, "$.members[0].id");
        post("/api/groups/" + groupId + "/members", owner, """
                {"email": "%s"}
                """.formatted(memberEmail)).andExpect(status().isCreated());
        post("/api/groups/" + groupId + "/expenses", owner, """
                {"description": "Палатка", "amount": 5000, "payerId": %d, "split": {"type": "EQUAL"}}
                """.formatted(anya)).andExpect(status().isCreated());

        delete("/api/groups/" + groupId, memberToken).andExpect(status().isForbidden());
        delete("/api/groups/" + groupId, owner).andExpect(status().isNoContent());

        get("/api/groups/" + groupId, owner).andExpect(status().isNotFound());
    }
}
