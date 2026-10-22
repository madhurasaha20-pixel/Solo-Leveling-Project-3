package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.exception.AccountForbiddenException;
import com.solo_leveling.bank_of_cli_backend.exception.AccountNotFoundException;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accounts;
    private final BankUserRepository users;
    private final AccountNumberGenerator accountNumbers;

    public AccountServiceImpl(AccountRepository accounts,
                              BankUserRepository users,
                              AccountNumberGenerator accountNumbers) {
        this.accounts = accounts;
        this.users = users;
        this.accountNumbers = accountNumbers;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(UUID ownerId, CreateAccountRequest request) {
        // the token's subject was checked against the database when it was authenticated
        BankUser owner = users.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + ownerId + " not found"));

        Account account = new Account();
        account.setId(accountNumbers.next());
        account.setUser(owner);
        account.setAccountType(request.type());
        if (request.nickname() != null && !request.nickname().isBlank()) {
            account.setNickname(request.nickname().trim());
        }
        return AccountResponse.from(accounts.saveAndFlush(account));
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
}
