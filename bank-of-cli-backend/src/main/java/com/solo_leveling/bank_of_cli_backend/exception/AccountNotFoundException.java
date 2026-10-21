package com.solo_leveling.bank_of_cli_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("Account " + accountId + " does not exist.");
    }

    private AccountNotFoundException() {
        super("No account found for this user.");
    }

    /** GET /accounts/me when the user hasn't opened an account yet. */
    public static AccountNotFoundException noAccountForUser() {
        return new AccountNotFoundException();
    }
}
