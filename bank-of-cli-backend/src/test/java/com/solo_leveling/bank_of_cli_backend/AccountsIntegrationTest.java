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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /accounts, GET /accounts/me and POST /accounts. */
class AccountsIntegrationTest extends ApiTestSupport {
    static final String ACCOUNTS = "/api/accounts";
    static final String PASSWORD = "correct-horse";

    @Autowired
    BankUserRepository users;
    @Autowired
    AccountRepository accounts;

    private String adaToken;
    private String graceToken;

    @BeforeEach
    void setUp() throws Exception {
        adaToken = field(registerAndGetBody("Ada", "ada@example.com", PASSWORD), "$.token");
        graceToken = field(registerAndGetBody("Grace", "grace@example.com", PASSWORD), "$.token");
    }

    private ResultActions create(String token, String json) throws Exception {
        return mockMvc.perform(post(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String createAndGetId(String token, String type) throws Exception {
        String body = create(token, """
                {"type": "%s"}
                """.formatted(type))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return field(body, "$.id");
    }

    private ResultActions getAs(String token, String path) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    // ---- POST /accounts ----

    @Test
    void createsAccountWithZeroBalance() throws Exception {
        String body = create(adaToken, """
                {"type": "savings", "nickname": "  Rainy day fund  "}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchesPattern("ACC-\\d{4,}")))
                .andExpect(jsonPath("$.type").value("savings"))
                .andExpect(jsonPath("$.nickname").value("Rainy day fund"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn().getResponse().getContentAsString();

        BankUser ada = users.findByEmail("ada@example.com").orElseThrow();
        Account saved = accounts.findById(field(body, "$.id")).orElseThrow();
        assertThat(saved.getUser().getId()).isEqualTo(ada.getId());
        assertThat(field(body, "$.userId")).isEqualTo(ada.getId().toString());
    }

    @Test
    void nicknameIsOptional() throws Exception {
        create(adaToken, """
                {"type": "checking", "nickname": "   "}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").doesNotExist());
    }

    @Test
    void givesEveryAccountItsOwnNumber() throws Exception {
        String first = createAndGetId(adaToken, "checking");
        String second = createAndGetId(adaToken, "checking");
        String third = createAndGetId(graceToken, "savings");

        assertThat(first).isNotEqualTo(second);
        assertThat(third).isNotIn(first, second);
    }

    @Test
    void skipsAccountNumbersThatAreAlreadyTaken() throws Exception {
        // claim the next few numbers by hand, like seed data in data.sql would
        String next = createAndGetId(adaToken, "checking");
        int n = Integer.parseInt(next.substring("ACC-".length()));
        BankUser grace = users.findByEmail("grace@example.com").orElseThrow();
        for (int i = 1; i <= 3; i++) {
            Account seeded = new Account();
            seeded.setId("ACC-%04d".formatted(n + i));
            seeded.setUser(grace);
            seeded.setAccountType("checking");
            accounts.saveAndFlush(seeded);
        }

        assertThat(createAndGetId(adaToken, "savings")).isEqualTo("ACC-%04d".formatted(n + 4));
    }

    @Test
    void rejectsMissingType() throws Exception {
        create(adaToken, """
                {"nickname": "Bills"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Account type is required."))
                .andExpect(jsonPath("$.field").value("type"));
    }

    @Test
    void rejectsUnknownType() throws Exception {
        create(adaToken, """
                {"type": "crypto"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Account type must be checking or savings."))
                .andExpect(jsonPath("$.field").value("type"));
    }

    @Test
    void rejectsNicknameOver30Characters() throws Exception {
        create(adaToken, """
                {"type": "checking", "nickname": "%s"}
                """.formatted("a".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("nickname"));

        BankUser ada = users.findByEmail("ada@example.com").orElseThrow();
        assertThat(accounts.findAllByOwner(ada.getId())).isEmpty();
    }

    // ---- GET /accounts ----

    @Test
    void listsOnlyYourAccountsOldestFirst() throws Exception {
        String checking = createAndGetId(adaToken, "checking");
        String savings = createAndGetId(adaToken, "savings");
        createAndGetId(graceToken, "checking");

        getAs(adaToken, ACCOUNTS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(checking))
                .andExpect(jsonPath("$[1].id").value(savings))
                .andExpect(jsonPath("$[1].type").value("savings"));
    }

    @Test
    void listIsEmptyBeforeOpeningAnAccount() throws Exception {
        getAs(adaToken, ACCOUNTS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- GET /accounts/me ----

    @Test
    void mainAccountIsTheFirstChecking() throws Exception {
        createAndGetId(adaToken, "savings");
        String checking = createAndGetId(adaToken, "checking");

        getAs(adaToken, ACCOUNTS + "/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checking))
                .andExpect(jsonPath("$.type").value("checking"));
    }

    @Test
    void mainAccountFallsBackToFirstAccount() throws Exception {
        String savings = createAndGetId(adaToken, "savings");

        getAs(adaToken, ACCOUNTS + "/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savings));
    }

    @Test
    void mainAccountIs404BeforeOpeningAnAccount() throws Exception {
        getAs(adaToken, ACCOUNTS + "/me")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No account found for this user."));
    }

    @Test
    void balanceShowsUpInTheList() throws Exception {
        String id = createAndGetId(adaToken, "checking");
        Account account = accounts.findById(id).orElseThrow();
        account.setBalance(new BigDecimal("42.50"));
        accounts.saveAndFlush(account);

        getAs(adaToken, ACCOUNTS)
                .andExpect(jsonPath("$[0].balance").value(42.50));
    }

    // ---- auth ----

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get(ACCOUNTS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ACCOUNTS + "/me")).andExpect(status().isUnauthorized());
        postJson(ACCOUNTS, """
                {"type": "checking"}
                """).andExpect(status().isUnauthorized());
    }
}
