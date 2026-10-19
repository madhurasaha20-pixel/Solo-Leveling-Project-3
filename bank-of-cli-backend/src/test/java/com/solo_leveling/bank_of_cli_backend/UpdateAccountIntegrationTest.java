package com.solo_leveling.bank_of_cli_backend;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UpdateAccountIntegrationTest extends ApiTestSupport {
    static final String PASSWORD = "correct-horse";

    @Autowired
    BankUserRepository users;
    @Autowired
    AccountRepository accounts;

    // login token of each registered user, by email
    private final Map<String, String> tokens = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        createUserWithAccount("Ada", "ada@example.com", "ACC-1001", "checking");
        createUserWithAccount("Grace", "grace@example.com", "ACC-1002", "checking");
    }

    // registers through the real endpoint to get a token, then gives the user an account
    private void createUserWithAccount(String name, String email, String accountId, String type) throws Exception {
        tokens.put(email, field(registerAndGetBody(name, email, PASSWORD), "$.token"));

        BankUser user = users.findByEmail(email).orElseThrow();
        Account account = new Account();
        account.setId(accountId);
        account.setUser(user);
        account.setAccountType(type);
        account.setBalance(new BigDecimal("25.00"));
        accounts.saveAndFlush(account);
    }

    private ResultActions update(String email, String accountId, String json) throws Exception {
        return mockMvc.perform(patch("/api/accounts/" + accountId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(email))
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
        assertThat(saved.getAccountType()).isEqualTo("savings");
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

        assertThat(accounts.findById("ACC-1001").orElseThrow().getAccountType()).isEqualTo("checking");
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
        mockMvc.perform(patch("/api/accounts/ACC-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "Bills"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
