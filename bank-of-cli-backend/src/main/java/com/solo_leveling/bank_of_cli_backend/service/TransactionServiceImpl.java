package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.DepositRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.TransferRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.WithdrawRequest;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.exception.AccountForbiddenException;
import com.solo_leveling.bank_of_cli_backend.exception.AccountNotFoundException;
import com.solo_leveling.bank_of_cli_backend.exception.InsufficientFundsException;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import com.solo_leveling.bank_of_cli_backend.dto.response.TransactionHistoryResponse;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final int MAX_HISTORY = 100;

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final EntityManager entityManager;
    private final TransferProcessor transferProcessor;

    public TransactionServiceImpl(
            AccountRepository accounts,
            TransactionRepository transactions,
            EntityManager entityManager,
            @Qualifier("internalTransferService") TransferProcessor transferProcessor
    ) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.entityManager = entityManager;
        this.transferProcessor = transferProcessor;
    }

    @Override
    @Transactional
    public Transaction deposit(
            String accountId,
            UUID ownerId,
            DepositRequest request
    ) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than zero"
            );
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
            throw new InsufficientFundsException();
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
    public Transaction transfer(
            String accountId,
            UUID ownerId,
            TransferRequest request
    ) {
        return transferProcessor.transfer(
                accountId,
                ownerId,
                request.destinationAccountId(),
                request.amount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getHistory(String accountId, UUID ownerId, int limit) {
        Account account = accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!account.getUser().getId().equals(ownerId)) {
            throw new AccountForbiddenException();
        }

        int size = Math.max(1, Math.min(limit, MAX_HISTORY));
        List<Transaction> newestFirst = transactions.findByAccount_IdOrderByCreatedAtDescIdDesc(accountId, PageRequest.of(0, size));

        BigDecimal running = account.getBalance();
        List<TransactionHistoryResponse> history = new ArrayList<>();
        for (Transaction transaction : newestFirst) {
            TransactionHistoryResponse item = TransactionHistoryResponse.from(transaction, running);
            history.add(item);

            boolean credit = "deposit".equals(item.type()) || "transfer-in".equals(item.type());
            running = credit
                    ? running.subtract(transaction.getAmount())
                    : running.add(transaction.getAmount());
        }
        return history;
    }
}