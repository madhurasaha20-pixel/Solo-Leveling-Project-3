package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.WireRequest;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WireService {

    private final AccountRepository accountRepository;

    @Autowired
    public WireService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void executeWireTransfer(String requestingUserId, WireRequest request) {

        String accountFrom = request.fromAccountId();
        String accountTo = request.toAccountId();
        BigDecimal amount = request.amount();

        if (accountFrom.equals(accountTo)) {
            throw new IllegalArgumentException("Source and destination must be different accounts.");
        }

        Account account1 = accountRepository.findById(accountFrom)
                .orElseThrow(() -> new AccessDeniedException("Source account not found or not accessible."));

        // TODO-SECURITY: restore the ownership check once register creates users
        // if (!account1.getUser().getId().toString().equals(requestingUserId)) {
        //     throw new AccessDeniedException("Source account not found or not accessible.");
        // }

        Account account2 = accountRepository.findById(accountTo)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found."));

        if (!account1.getCurrency().equals(account2.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts.");
        }

        if (account1.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds.");
        }

        account1.setBalance(account1.getBalance().subtract(amount));
        account2.setBalance(account2.getBalance().add(amount));

        accountRepository.save(account1);
        accountRepository.save(account2);
    }
}