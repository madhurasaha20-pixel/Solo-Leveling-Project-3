package com.solo_leveling.bank_of_cli_backend.dto.response;

import com.solo_leveling.bank_of_cli_backend.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryResponse(
        String id,
        String accountId,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant date
) {
    public static TransactionHistoryResponse from(Transaction transaction, BigDecimal balanceAfter) {
        return new TransactionHistoryResponse(
                transaction.getId().toString(),
                transaction.getAccount().getId(),
                toContractType(transaction.getTransactionType()),
                transaction.getAmount(),
                balanceAfter,
                transaction.getCreatedAt()
        );
    }

    private static String toContractType(String databaseType) {
        return switch (databaseType) {
            case "DEPOSIT" -> "deposit";
            case "WITHDRAWAL" -> "withdraw";
            case "TRANSFER_IN" -> "transfer-in";
            case "TRANSFER_OUT" -> "transfer-out";
            default -> throw new IllegalStateException("Unknown transaction type: " + databaseType);
        };
    }
}