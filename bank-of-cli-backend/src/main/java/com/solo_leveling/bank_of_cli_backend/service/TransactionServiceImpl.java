package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.DepositRequest;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import com.solo_leveling.bank_of_cli_backend.exception.AccountNotFoundException;
import com.solo_leveling.bank_of_cli_backend.exception.AccountForbiddenException;
import com.solo_leveling.bank_of_cli_backend.dto.request.WithdrawRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.TransferRequest;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final EntityManager entityManager;

    public TransactionServiceImpl(
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
    public Transaction deposit(
            String accountId,
            UUID ownerId,
            DepositRequest request
    ) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!account.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        account.setBalance(account.getBalance().add(request.amount()));
        accounts.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(request.amount());

        Transaction savedTransaction = transactions.saveAndFlush(transaction);
        entityManager.refresh(savedTransaction);
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction withdraw(
            String accountId,
            UUID ownerId,
            WithdrawRequest request
    ) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be greater than zero"
            );
        }

        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!account.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));
        accounts.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAmount(request.amount());

        Transaction savedTransaction = transactions.saveAndFlush(transaction);
        entityManager.refresh(savedTransaction);
        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction transfer(
            String accountId,
            UUID ownerId,
            TransferRequest request
    ) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        if (accountId.equals(request.destinationAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }

        Account source = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!source.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        Account destination = accounts.findById(request.destinationAccountId())
                .orElseThrow(() ->
                        new AccountNotFoundException(request.destinationAccountId()));

        if (!destination.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));

        accounts.save(source);
        accounts.save(destination);

        Transaction outgoing = new Transaction();
        outgoing.setAccount(source);
        outgoing.setTransactionType("TRANSFER_OUT");
        outgoing.setAmount(request.amount());

        Transaction incoming = new Transaction();
        incoming.setAccount(destination);
        incoming.setTransactionType("TRANSFER_IN");
        incoming.setAmount(request.amount());

        Transaction savedOutgoing = transactions.saveAndFlush(outgoing);
        transactions.saveAndFlush(incoming);

        entityManager.refresh(savedOutgoing);
        return savedOutgoing;
    }
}