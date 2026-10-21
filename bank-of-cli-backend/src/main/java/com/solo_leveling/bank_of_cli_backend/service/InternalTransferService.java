package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.exception.AccountForbiddenException;
import com.solo_leveling.bank_of_cli_backend.exception.AccountNotFoundException;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class InternalTransferService implements TransferProcessor {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final EntityManager entityManager;

    public InternalTransferService(
            AccountRepository accounts,
            TransactionRepository transactions,
            EntityManager entityManager
    ) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Transaction transfer(
            String accountId,
            UUID ownerId,
            String destinationAccountId,
            BigDecimal amount
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        if (accountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }

        Account source = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!source.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        Account destination = accounts.findById(destinationAccountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(destinationAccountId));

        if (!destination.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        accounts.save(source);
        accounts.save(destination);

        Transaction outgoing = new Transaction();
        outgoing.setAccount(source);
        outgoing.setTransactionType("TRANSFER_OUT");
        outgoing.setAmount(amount);

        Transaction incoming = new Transaction();
        incoming.setAccount(destination);
        incoming.setTransactionType("TRANSFER_IN");
        incoming.setAmount(amount);

        Transaction savedOutgoing = transactions.saveAndFlush(outgoing);
        transactions.saveAndFlush(incoming);

        entityManager.refresh(savedOutgoing);
        return savedOutgoing;
    }
}