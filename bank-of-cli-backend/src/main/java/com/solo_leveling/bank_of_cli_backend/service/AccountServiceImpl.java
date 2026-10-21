package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.CreateAccountResponse;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.exception.AccountForbiddenException;
import com.solo_leveling.bank_of_cli_backend.exception.AccountNotFoundException;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final BankUserRepository bankUsers;
    private final JdbcTemplate jdbcTemplate;

    public AccountServiceImpl(AccountRepository accounts, BankUserRepository bankUsers, JdbcTemplate jdbcTemplate) {
        this.accounts = accounts;
        this.bankUsers = bankUsers;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(UUID ownerId) {
        List<AccountResponse> mine = getMyAccounts(ownerId);
        return mine.stream()
                .filter(account -> "checking".equals(account.type()))
                .findFirst()
                .or(() -> mine.stream().findFirst())
                .orElseThrow(AccountNotFoundException::forCurrentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(UUID ownerId) {
        return accounts.findAllByUser_IdOrderByCreatedAtAscIdAsc(ownerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            account.setNickname(nickname.isEmpty() ? null : nickname);
        }
        if (request.type() != null) {
            account.setAccountType(request.type());
        }

        return AccountResponse.from(accounts.save(account));
    }

    public CreateAccountResponse addNewAccount(
            CreateAccountRequest request,
            UUID ownerId) {

        CreateAccountResponse response = new CreateAccountResponse();

        // Find the authenticated user using their UUID
        Optional<BankUser> bankUserOptional =
                bankUsers.findById(ownerId);

        String currencyCode = request.getCurrencyForNewAccount();
        String nickname = request.getNicknameForNewAccount();
        String accountType = request.getAccountTypeForNewAccount();

        if (bankUserOptional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Could not find the authenticated user");
            return response;
        }

        BankUser bankUser = bankUserOptional.get();

        // The rest of your validation and account creation logic
        // can remain the same.

        // Validate account type
        // String accountType = request.getAccountTypeForNewAccount();
        if (!"checking".equals(accountType)
                && !"savings".equals(accountType)) {
            response.setSuccess(false);
            response.setMessage("Invalid account type");
            return response;
        }

        // Validate nickname
        //String nickname = request.getNicknameForNewAccount();

        if (nickname == null || nickname.isBlank()) {
            response.setSuccess(false);
            response.setMessage("Nickname is required");
            return response;
        }

        // Check for duplicate nickname belonging to this user
        boolean nickNameExists =
                accounts.existsByUserAndNickname(bankUser, nickname);

        if (nickNameExists) {
            response.setSuccess(false);
            response.setMessage(
                    "There is already an account with this nickname"
            );
            return response;
        }

        // Validate currency
        //String currencyCode = request.getCurrencyForNewAccount();

        if (currencyCode == null || currencyCode.isBlank()) {
            response.setSuccess(false);
            response.setMessage("Currency not given");
            return response;
        }

        currencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            response.setSuccess(false);
            response.setMessage("Invalid currency");
            return response;
        }

        // Create the new account
        Account newAccount = new Account();

        // Generate an ID
        Long nextNumber = jdbcTemplate.queryForObject(
                "SELECT NEXT VALUE FOR account_id_seq",
                Long.class
        );

        newAccount.setId(String.format(Locale.ROOT, "ACC-%04d", nextNumber));

        newAccount.setAccountType(accountType);
        newAccount.setCurrency(currencyCode);
        newAccount.setNickname(nickname);
        newAccount.setUser(bankUser);

        accounts.save(newAccount);

        // Return success
        response.setSuccess(true);
        response.setMessage("Account created successfully");

        return response;
    }

}
