package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransferProcessor {
    Transaction transfer(
            String accountId,
            UUID ownerId,
            String destinationAccountId,
            BigDecimal amount
    );
}