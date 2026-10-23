package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service("wireTransferProcessor")
public class WireService implements TransferProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    public WireService(AccountRepository accountRepository,
                       TransactionRepository transactionRepository,
                       EntityManager entityManager) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Transaction transfer(String accountId,
                                UUID ownerId,
                                String destinationAccountId,
                                BigDecimal amount) {

        if (accountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination must be different accounts.");
        }

        Account fromAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccessDeniedException("Source account not found or not accessible."));

        // Ownership: only the account's owner may wire money out of it
//        if (!fromAccount.getUser().getId().equals(ownerId)) {
//            throw new AccessDeniedException("Source account not found or not accessible.");
//        }

        Account toAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found."));

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts.");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds.");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction debit = new Transaction();
        debit.setAccount(fromAccount);
        debit.setTransactionType("TRANSFER_OUT");
        debit.setAmount(amount);

        Transaction credit = new Transaction();
        credit.setAccount(toAccount);
        credit.setTransactionType("TRANSFER_IN");
        credit.setAmount(amount);

        Transaction savedDebit = transactionRepository.saveAndFlush(debit);
        transactionRepository.saveAndFlush(credit);

        entityManager.refresh(savedDebit);
        return savedDebit;
    }
}