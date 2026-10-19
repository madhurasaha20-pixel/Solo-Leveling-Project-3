package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class WireService {

    public List<Account> executeWireTransfer() {
        // Validate accounts and transfer rules
        // Debit the sender's account
        // Credit the recipient's account
        // Record the transaction
        // Return the transfer result

        List<Account> accounts = List.of(
                new Account("ACC001", "CHECKING", "Main Checking", new BigDecimal("2500.00"), "USD", Instant.now(), Instant.now()),

                new Account("ACC002", "SAVINGS", "Emergency Savings", new BigDecimal("10000.50"), "USD", Instant.now(), Instant.now()),

                new Account("ACC003", "CHECKING", "Travel Fund", new BigDecimal("750.25"), "EUR", Instant.now(), Instant.now())
        );

        return accounts;
    }

}
