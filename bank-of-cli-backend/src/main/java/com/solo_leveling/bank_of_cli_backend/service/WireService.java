package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service("wireTransferProcessor")
public class WireService implements TransferProcessor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WireService(AccountRepository accountRepository,
                       TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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

        Account from = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccessDeniedException("Source account not found or not accessible."));


        Account to = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found."));

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts.");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds.");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);


        Transaction debit = new Transaction();
        debit.setAccount(from);
        debit.setTransactionType("TRANSFER_OUT");
        debit.setAmount(amount);


        Transaction credit = new Transaction();
        credit.setAccount(to);
        credit.setTransactionType("TRANSFER_IN");
        credit.setAmount(amount);

        transactionRepository.save(credit);
        return transactionRepository.save(debit);
    }
}