package com.solo_leveling.bank_of_cli_backend;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** POST /api/auth/logout is used as the "any protected endpoint" probe. */
class ProtectedAccessIntegrationTest extends ApiTestSupport {
    @Value("${app.jwt.secret}")
    String secret;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager entityManager;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(post(LOGOUT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() throws Exception {
        String body = registerAndGetBody("Ada", "ada@example.com", "correct-horse");
        String forged = forgeToken(field(body, "$.user.id"), "a-completely-different-secret-0123456789");

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void acceptsTokenSignedWithConfiguredSecret() throws Exception {
        // proves the test helper matches the real signing scheme (HS256 over app.jwt.secret)
        String body = registerAndGetBody("Ada", "ada@example.com", "correct-horse");
        String token = forgeToken(field(body, "$.user.id"), secret);

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String body = registerAndGetBody("Ada", "ada@example.com", "correct-horse");
        String[] parts = field(body, "$.token").split("\\.");
        String otherUserPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\"}").getBytes());
        String tampered = parts[0] + "." + otherUserPayload + "." + parts[2];

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsValidTokenWhoseUserNoLongerExists() throws Exception {
        String body = registerAndGetBody("Ada", "ada@example.com", "correct-horse");
        jdbc.update("delete from bank_user where id = ?", UUID.fromString(field(body, "$.user.id")));
        entityManager.clear();

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer(field(body, "$.token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownPathReturnsNotFoundBody() throws Exception {
        String token = field(registerAndGetBody("Ada", "ada@example.com", "correct-horse"), "$.token");

        mockMvc.perform(get("/api/does-not-exist").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void logoutReturnsNoContentWithValidToken() throws Exception {
        String token = field(registerAndGetBody("Ada", "ada@example.com", "correct-horse"), "$.token");

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
