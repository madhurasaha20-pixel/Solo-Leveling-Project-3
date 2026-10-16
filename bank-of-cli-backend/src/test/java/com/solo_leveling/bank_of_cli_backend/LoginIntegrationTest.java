package com.solo_leveling.bank_of_cli_backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginIntegrationTest extends ApiTestSupport {
    private static final String EMAIL = "ada@example.com";
    private static final String PASSWORD = "correct-horse";

    @BeforeEach
    void registerUser() throws Exception {
        register("Ada Lovelace", EMAIL, PASSWORD).andExpect(status().isCreated());
    }

    @Test
    void returnsTokenAndUserForValidCredentials() throws Exception {
        postJson(LOGIN, loginBody(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginTokenAuthenticatesProtectedRequests() throws Exception {
        String body = postJson(LOGIN, loginBody(EMAIL, PASSWORD))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Bearer " + field(body, "$.token")))
                .andExpect(status().isNoContent());
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        postJson(LOGIN, loginBody("ADA@Example.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        postJson(LOGIN, loginBody(EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Incorrect email or password."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void unknownEmailLooksIdenticalToWrongPassword() throws Exception {
        postJson(LOGIN, loginBody("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Incorrect email or password."));
    }

    @Test
    void rejectsEmptyEmail() throws Exception {
        postJson(LOGIN, loginBody("", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsEmptyPassword() throws Exception {
        postJson(LOGIN, loginBody(EMAIL, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("password"));
    }

    @Test
    void httpBasicIsNoLongerAccepted() throws Exception {
        String basic = Base64.getEncoder().encodeToString((EMAIL + ":" + PASSWORD).getBytes());

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Basic " + basic))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void staleTokenDoesNotBlockLogin() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer stale.invalid.token")
                        .contentType("application/json")
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void staleTokenDoesNotBlockRegister() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer stale.invalid.token")
                        .contentType("application/json")
                        .content(registerBody("Grace", "grace@example.com", "correct-horse")))
                .andExpect(status().isCreated());
    }
}
