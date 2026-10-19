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
        //Grab Accounts from database


        //Set accounts to variables
        Account account1 = accounts.get(0);
        Account account2 = accounts.get(1);
        BigDecimal amount = BigDecimal.valueOf(500);

        // Validate accounts and transfer rules
        // Debit the sender's account
        account1.setBalance(account1.getBalance().subtract(amount));
        // Credit the recipient's account
        account2.setBalance(account2.getBalance().add(amount));
        // Record the transaction
        // Return the transfer result
        return accounts;
    }

}
