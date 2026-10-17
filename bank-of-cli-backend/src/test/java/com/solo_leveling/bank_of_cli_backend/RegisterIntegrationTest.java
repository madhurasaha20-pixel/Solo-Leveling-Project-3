package com.solo_leveling.bank_of_cli_backend;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegisterIntegrationTest extends ApiTestSupport {
    @Autowired
    BankUserRepository users;
    @Autowired
    UserCredentialRepository credentials;
    @Autowired
    UserRoleRepository userRoles;

    @Test
    void registersUserWithHashedPasswordAndCustomerRole() throws Exception {
        register("Ada Lovelace", "ada@example.com", "correct-horse").andExpect(status().isCreated());

        BankUser user = users.findByEmail("ada@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Ada Lovelace");

        UserCredential credential = credentials.findById(user.getId()).orElseThrow();
        assertThat(credential.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("correct-horse");

        assertThat(userRoles.findByUserId(user.getId()))
                .extracting(userRole -> userRole.getRole().getName())
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void returnsTokenAndUserSoTheNewUserIsLoggedIn() throws Exception {
        register("Ada Lovelace", "ada@example.com", "correct-horse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void userIdIsTheStoredUuid() throws Exception {
        String body = registerAndGetBody("Ada", "ada@example.com", "correct-horse");

        UUID returnedId = UUID.fromString(field(body, "$.user.id"));
        assertThat(users.findByEmail("ada@example.com").orElseThrow().getId()).isEqualTo(returnedId);
    }

    @Test
    void returnedTokenAuthenticatesProtectedRequests() throws Exception {
        String token = field(registerAndGetBody("Ada", "ada@example.com", "correct-horse"), "$.token");

        mockMvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void acceptsSixCharacterPassword() throws Exception {
        register("Ada", "ada@example.com", "abcdef").andExpect(status().isCreated());
    }

    @Test
    void lowercasesEmailBeforeStoring() throws Exception {
        register("Ada", "Ada@Example.COM", "correct-horse").andExpect(status().isCreated());

        assertThat(users.findByEmail("ada@example.com")).isPresent();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register("Ada", "ada@example.com", "correct-horse").andExpect(status().isCreated());

        register("Someone Else", "ADA@example.com", "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsTooShortPassword() throws Exception {
        register("Ada", "ada@example.com", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("password"));

        assertThat(users.findByEmail("ada@example.com")).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        register("Ada", "not-an-email", "correct-horse")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsEmailWithSurroundingWhitespace() throws Exception {
        register("Ada", " ada@example.com ", "correct-horse")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        register("   ", "ada@example.com", "correct-horse")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson(REGISTER, "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void legacyUnprefixedPathIsGone() throws Exception {
        postJson("/auth/register", registerBody("Ada", "ada@example.com", "correct-horse"))
                .andExpect(status().is4xxClientError());

        assertThat(users.findByEmail("ada@example.com")).isEmpty();
    }
}
