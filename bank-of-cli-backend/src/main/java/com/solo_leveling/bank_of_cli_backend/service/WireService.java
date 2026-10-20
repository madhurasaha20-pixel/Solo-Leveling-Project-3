package com.solo_leveling.bank_of_cli_backend.service;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;


//Create the service class and make it a bean

@Service
public class WireService {
    //Define Injected vars & Inject user DAO into wire service/ use construct injection
    private final AccountRepository accountRepository;

    @Autowired
    public WireService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    //Define transfer method
    public void executeWireTransfer(String accountFrom, String accountTo, BigDecimal amount) {
        // Validate accounts and transfer rules
        //Take a string and convert it
        Account account1 = accountRepository.findById(accountFrom).orElseThrow(() -> new RuntimeException("Source not found."));
        Account account2 = accountRepository.findById(accountTo).orElseThrow(() -> new RuntimeException("Source not found."));

        //Use the entity setter to change the balance
        account1.setBalance(account1.getBalance().subtract(amount));
        account2.setBalance(account2.getBalance().subtract(amount));
        //call user DAOs save method
        accountRepository.save(account1);
        accountRepository.save(account2);
    }

}
