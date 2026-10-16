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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Currency;
import java.util.Locale;


@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final BankUserRepository bankUsers;

    public AccountServiceImpl(AccountRepository accounts, BankUserRepository bankUsers) {
        this.accounts = accounts;
        this.bankUsers = bankUsers;
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(String accountId, String ownerEmail, UpdateAccountRequest request) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        // the principal name is the stored (already lowercased) email
        if (!account.getUser().getEmail().equals(ownerEmail)) {
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
            String email) {

        CreateAccountResponse response = new CreateAccountResponse();

        // Find the authenticated user using their email
        Optional<BankUser> bankUserOptional = bankUsers.findByEmail(email);

        String currencyCode = request.getCurrencyForNewAccount();
        String nickname = request.getNicknameForNewAccount();
        String accountType = request.getAccountTypeForNewAccount();

        System.out.println("Account type printed: "+ accountType);
        System.out.println("Nickname printed: "+ nickname);
        System.out.println("Currency given: " + currencyCode);

        if (bankUserOptional.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Could not find the authenticated user");
            return response;
        }

        BankUser bankUser = bankUserOptional.get();

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

        // Generate an ID on the backend
        newAccount.setIdForAccountModel(
                "ACC-" + UUID.randomUUID()
        );

        newAccount.setAccountTypeForAccountModel(accountType);
        newAccount.setCurrencyForAccountModel(currencyCode);
        newAccount.setNicknameForAccountModel(nickname);
        newAccount.setUserForAccountModel(bankUser);

        accounts.save(newAccount);

        // Return success
        response.setSuccess(true);
        response.setMessage("Account created successfully");

        return response;
    }

}
