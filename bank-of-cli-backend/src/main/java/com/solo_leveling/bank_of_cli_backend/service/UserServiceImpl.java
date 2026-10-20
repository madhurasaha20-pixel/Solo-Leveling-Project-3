package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.DeleteUserRequest;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import com.solo_leveling.bank_of_cli_backend.exception.BalanceNotZeroException;
import com.solo_leveling.bank_of_cli_backend.exception.IncorrectPasswordException;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final BankUserRepository users;
    private final UserCredentialRepository credentials;
    private final UserRoleRepository userRoles;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(BankUserRepository users,
                           UserCredentialRepository credentials,
                           UserRoleRepository userRoles,
                           AccountRepository accounts,
                           TransactionRepository transactions,
                           PasswordEncoder passwordEncoder) {
        this.users = users;
        this.credentials = credentials;
        this.userRoles = userRoles;
        this.accounts = accounts;
        this.transactions = transactions;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, DeleteUserRequest request) {
        // the token was already checked, so a missing row means the user is gone; treat like a bad password
        UserCredential credential = credentials.findById(userId).orElseThrow(IncorrectPasswordException::new);
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }

        // a bank doesn't make money disappear: every account must be emptied first
        List<Account> funded = accounts.findAllByOwnerForUpdate(userId).stream()
                .filter(account -> account.getBalance().signum() > 0)
                .toList();
        if (!funded.isEmpty()) {
            boolean allUsd = funded.stream().allMatch(account -> "USD".equals(account.getCurrency()));
            BigDecimal total = funded.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
            throw new BalanceNotZeroException(allUsd ? total : null);
        }

        // children first: bank_transaction -> account has no ON DELETE CASCADE in schema.sql
        transactions.deleteAllByAccountOwner(userId);
        accounts.deleteAllByOwner(userId);
        userRoles.deleteAllByUserId(userId);
        credentials.deleteAllByUserId(userId);
        users.deleteByIdNow(userId);
        // any token this user still holds now fails with 401: UserIdJwtAuthenticationConverter checks the user exists
    }
}