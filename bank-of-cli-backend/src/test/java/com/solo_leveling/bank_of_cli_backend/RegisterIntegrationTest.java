package com.solo_leveling.bank_of_cli_backend;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back after each test so tests don't share users
class RegisterIntegrationTest {
    @Autowired // field injection on tests is standard
    MockMvc mockMvc;
    @Autowired
    BankUserRepository users;
    @Autowired
    UserCredentialRepository credentials;
    @Autowired
    UserRoleRepository userRoles;

    private static String body(String name, String email, String password) {
        return """
                {"name": "%s", "email": "%s", "password": "%s"}
                """.formatted(name, email, password);
    }

    private ResultActions register(String json) throws Exception {
        return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void registersUserWithHashedPasswordAndCustomerRole() throws Exception {
        register(body("Ada Lovelace", "ada@example.com", "correct-horse")).andExpect(status().isCreated());

        BankUser user = users.findByEmail("ada@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Ada Lovelace");
        assertThat(user.getId()).isNotNull();

        UserCredential credential = credentials.findById(user.getId()).orElseThrow();
        assertThat(credential.getPasswordHash())
                .startsWith("{bcrypt}")
                .doesNotContain("correct-horse");

        assertThat(userRoles.findByUserId(user.getId()))
                .extracting(userRole -> userRole.getRole().getName())
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void lowercasesEmailBeforeStoring() throws Exception {
        register(body("Ada", "Ada@Example.COM", "correct-horse")).andExpect(status().isCreated());

        assertThat(users.findByEmail("ada@example.com")).isPresent();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register(body("Ada", "ada@example.com", "correct-horse")).andExpect(status().isCreated());

        register(body("Someone Else", "ADA@example.com", "another-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsShortPassword() throws Exception {
        register(body("Ada", "ada@example.com", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("password"));

        assertThat(users.findByEmail("ada@example.com")).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        register(body("Ada", "not-an-email", "correct-horse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsEmailWithSurroundingWhitespace() throws Exception {
        register(body("Ada", " ada@example.com ", "correct-horse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        register(body("   ", "ada@example.com", "correct-horse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        register("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
