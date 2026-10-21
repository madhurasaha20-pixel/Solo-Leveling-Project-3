package com.solo_leveling.bank_of_cli_backend;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeleteUserIntegrationTest extends ApiTestSupport {
    static final String DELETE_ME = "/api/users/me";
    static final String PASSWORD = "correct-horse";

    @Autowired BankUserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;
    @Autowired UserCredentialRepository credentials;
    @Autowired UserRoleRepository userRoles;

    private final Map<String, String> tokens = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        // both start emptied out ($50 in, $50 out), so they are allowed to delete; history still exists
        createUserWithEmptiedAccount("Ada", "ada@example.com", "ACC-1001");
        createUserWithEmptiedAccount("Grace", "grace@example.com", "ACC-1002");
    }

    private void createUserWithEmptiedAccount(String name, String email, String accountId) throws Exception {
        tokens.put(email, field(registerAndGetBody(name, email, PASSWORD), "$.token"));

        BankUser user = users.findByEmail(email).orElseThrow();
        Account account = addAccount(user, accountId, "0.00", "USD");
        addTransaction(account, "DEPOSIT", "50.00");
        addTransaction(account, "WITHDRAWAL", "50.00");
    }

    private Account addAccount(BankUser user, String accountId, String balance, String currency) {
        Account account = new Account();
        account.setId(accountId);
        account.setUser(user);
        account.setAccountType("checking");
        account.setBalance(new BigDecimal(balance));
        account.setCurrency(currency);
        return accounts.saveAndFlush(account);
    }

    private void addTransaction(Account account, String type, String amount) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(new BigDecimal(amount));
        transactions.saveAndFlush(transaction);
    }

    private void giveAdaMoney(String balance, String currency) {
        BankUser ada = users.findByEmail("ada@example.com").orElseThrow();
        addAccount(ada, "ACC-1003", balance, currency);
    }

    private ResultActions deleteMe(String token, String json) throws Exception {
        var request = delete(DELETE_ME).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return mockMvc.perform(request);
    }

    private static String passwordBody(String password) {
        return """
                {"password": "%s"}
                """.formatted(password);
    }

    @Test
    void deletesUserAndEverythingTheyOwn() throws Exception {
        UUID adaId = users.findByEmail("ada@example.com").orElseThrow().getId();

        deleteMe(tokens.get("ada@example.com"), passwordBody(PASSWORD))
                .andExpect(status().isNoContent());

        assertThat(users.existsById(adaId)).isFalse();
        assertThat(credentials.existsById(adaId)).isFalse();
        assertThat(userRoles.findByUserId(adaId)).isEmpty();
        assertThat(accounts.existsById("ACC-1001")).isFalse();
        assertThat(transactions.findAll())
                .noneMatch(t -> t.getAccount().getId().equals("ACC-1001"));
    }

    @Test
    void leavesOtherUsersAlone() throws Exception {
        deleteMe(tokens.get("ada@example.com"), passwordBody(PASSWORD))
                .andExpect(status().isNoContent());

        assertThat(users.findByEmail("grace@example.com")).isPresent();
        assertThat(accounts.existsById("ACC-1002")).isTrue();
        assertThat(transactions.findAll())
                .anyMatch(t -> t.getAccount().getId().equals("ACC-1002"));
    }

    @Test
    void oldTokenAndLoginStopWorkingAfterDelete() throws Exception {
        String token = tokens.get("ada@example.com");
        deleteMe(token, passwordBody(PASSWORD)).andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/accounts/ACC-1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"ghost\"}"))
                .andExpect(status().isUnauthorized());

        postJson(LOGIN, loginBody("ada@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void emailCanBeReusedAfterDelete() throws Exception {
        deleteMe(tokens.get("ada@example.com"), passwordBody(PASSWORD)).andExpect(status().isNoContent());
        register("Ada Again", "ada@example.com", "new-password").andExpect(status().isCreated());
    }

    @Test
    void refusesWhileAnAccountHoldsMoney() throws Exception {
        giveAdaMoney("1325.75", "USD");

        deleteMe(tokens.get("ada@example.com"), passwordBody(PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("BALANCE_NOT_ZERO"))
                .andExpect(jsonPath("$.message").value(
                        "You still have $1,325.75 in your accounts. "
                                + "Withdraw or transfer it first, so every account is at $0."));

        // nothing was deleted, not even the emptied account or its history
        assertThat(users.findByEmail("ada@example.com")).isPresent();
        assertThat(accounts.existsById("ACC-1001")).isTrue();
        assertThat(accounts.existsById("ACC-1003")).isTrue();
        assertThat(transactions.findAll()).anyMatch(t -> t.getAccount().getId().equals("ACC-1001"));
    }

    @Test
    void leavesOutTheAmountWhenCurrenciesAreMixed() throws Exception {
        giveAdaMoney("10.00", "EUR");

        deleteMe(tokens.get("ada@example.com"), passwordBody(PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BALANCE_NOT_ZERO"))
                .andExpect(jsonPath("$.message").value(
                        "Your accounts still have money in them. "
                                + "Withdraw or transfer it first, so every account is at $0."));
    }

    @Test
    void checksPasswordBeforeBalance() throws Exception {
        giveAdaMoney("5.00", "USD");

        // a wrong password must not reveal anything about the balance
        deleteMe(tokens.get("ada@example.com"), passwordBody("wrong-password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("password"));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        deleteMe(tokens.get("ada@example.com"), passwordBody("wrong-password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Incorrect password."))
                .andExpect(jsonPath("$.field").value("password"));

        assertThat(users.findByEmail("ada@example.com")).isPresent();
        assertThat(accounts.existsById("ACC-1001")).isTrue();
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        deleteMe(tokens.get("ada@example.com"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("password"));

        assertThat(users.findByEmail("ada@example.com")).isPresent();
    }

    @Test
    void requiresLogin() throws Exception {
        deleteMe(null, passwordBody(PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        assertThat(users.findByEmail("ada@example.com")).isPresent();
    }
}