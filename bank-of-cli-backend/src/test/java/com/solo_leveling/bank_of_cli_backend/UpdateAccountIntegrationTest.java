package com.solo_leveling.bank_of_cli_backend;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back after each test so tests don't share users or accounts
class UpdateAccountIntegrationTest {
    static final String PASSWORD = "correct-horse";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    BankUserRepository users;
    @Autowired
    AccountRepository accounts;

    @BeforeEach
    void setUp() throws Exception {
        createUserWithAccount("Ada", "ada@example.com", "ACC-1001", "checking");
        createUserWithAccount("Grace", "grace@example.com", "ACC-1002", "checking");
    }

    // registers through the real endpoint so the login works, then gives the user an account
    private void createUserWithAccount(String name, String email, String accountId, String type) throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "%s", "email": "%s", "password": "%s"}
                        """.formatted(name, email, PASSWORD)))
                .andExpect(status().isCreated());

        BankUser user = users.findByEmail(email).orElseThrow();
        Account account = new Account();
        account.setId(accountId);
        account.setUser(user);
        account.setType(type);
        account.setBalance(new BigDecimal("25.00"));
        accounts.saveAndFlush(account);
    }

    private ResultActions update(String email, String accountId, String json) throws Exception {
        return mockMvc.perform(patch("/accounts/" + accountId)
                .with(httpBasic(email, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void updatesNicknameAndType() throws Exception {
        update("ada@example.com", "ACC-1001", """
                {"nickname": "  Rainy day fund  ", "type": "savings"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ACC-1001"))
                .andExpect(jsonPath("$.nickname").value("Rainy day fund"))
                .andExpect(jsonPath("$.type").value("savings"))
                .andExpect(jsonPath("$.balance").value(25.00))
                .andExpect(jsonPath("$.currency").value("USD"));

        Account saved = accounts.findById("ACC-1001").orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("Rainy day fund");
        assertThat(saved.getType()).isEqualTo("savings");
    }

    @Test
    void leavesFieldsThatAreLeftOutUnchanged() throws Exception {
        update("ada@example.com", "ACC-1001", """
                {"nickname": "Bills"}
                """).andExpect(status().isOk());

        update("ada@example.com", "ACC-1001", """
                {"type": "savings"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Bills"))
                .andExpect(jsonPath("$.type").value("savings"));
    }

    @Test
    void blankNicknameClearsIt() throws Exception {
        update("ada@example.com", "ACC-1001", """
                {"nickname": "Bills"}
                """).andExpect(status().isOk());

        update("ada@example.com", "ACC-1001", """
                {"nickname": "   "}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").doesNotExist());

        assertThat(accounts.findById("ACC-1001").orElseThrow().getNickname()).isNull();
    }

    @Test
    void rejectsNicknameOver30Characters() throws Exception {
        update("ada@example.com", "ACC-1001", """
                {"nickname": "%s"}
                """.formatted("a".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Nickname must be 30 characters or fewer."))
                .andExpect(jsonPath("$.field").value("nickname"));
    }

    @Test
    void rejectsUnknownType() throws Exception {
        update("ada@example.com", "ACC-1001", """
                {"type": "crypto"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("type"));

        assertThat(accounts.findById("ACC-1001").orElseThrow().getType()).isEqualTo("checking");
    }

    @Test
    void rejectsSomeoneElsesAccount() throws Exception {
        update("ada@example.com", "ACC-1002", """
                {"nickname": "Mine now"}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(accounts.findById("ACC-1002").orElseThrow().getNickname()).isNull();
    }

    @Test
    void rejectsMissingAccount() throws Exception {
        update("ada@example.com", "ACC-9999", """
                {"nickname": "Ghost"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account ACC-9999 does not exist."));
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(patch("/accounts/ACC-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "Bills"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
