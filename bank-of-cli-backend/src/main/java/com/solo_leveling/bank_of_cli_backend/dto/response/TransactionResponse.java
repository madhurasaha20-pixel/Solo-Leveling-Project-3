package com.solo_leveling.bank_of_cli_backend.dto.response;

import com.solo_leveling.bank_of_cli_backend.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String accountId,
        String transactionType,
        BigDecimal amount,
        BigDecimal newBalance,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getAccount().getBalance(),
                transaction.getCreatedAt()
        );
    }
}