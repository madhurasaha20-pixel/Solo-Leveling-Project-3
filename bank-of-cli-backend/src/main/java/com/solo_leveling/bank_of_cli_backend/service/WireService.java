package com.solo_leveling.bank_of_cli_backend.service;

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
    public void executeWireTransfer(String requestingUserId,
                                    String accountFrom,
                                    String accountTo,
                                    BigDecimal amount) {

        // RULE 1: inputs must exist
        if (requestingUserId == null || requestingUserId.isBlank()) {
            throw new AccessDeniedException("Authentication required.");
        }
        if (accountFrom == null || accountFrom.isBlank()
                || accountTo == null || accountTo.isBlank()) {
            throw new IllegalArgumentException("Both account ids are required.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required.");
        }


        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }


        if (accountFrom.equals(accountTo)) {
            throw new IllegalArgumentException("Source and destination must be different accounts.");
        }


        Account account1 = accountRepository.findById(accountFrom)
                .orElseThrow(() -> new AccessDeniedException("Source account not found or not accessible."));

        //Leaving this  commented out until we can add users to DB through register
//        if (!account1.getUser().getId().toString().equals(requestingUserId)) {
//            throw new AccessDeniedException("Source account not found or not accessible.");
//        }

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