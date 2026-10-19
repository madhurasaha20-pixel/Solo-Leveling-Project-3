package com.solo_leveling.bank_of_cli_backend;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileIntegrationTest extends ApiTestSupport {
    @Test
    void returnsOnlyTheAuthenticatedUsersPublicDetails() throws Exception {
        String body = registerAndGetBody("Taylor", "taylor@example.com", "password123");
        registerAndGetBody("Other User", "other@example.com", "password123");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + field(body, "$.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(field(body, "$.user.id")))
                .andExpect(jsonPath("$.name").value("Taylor"))
                .andExpect(jsonPath("$.email").value("taylor@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}
