package com.solo_leveling.bank_of_cli_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// not Spring Security's AccessDeniedException, so GlobalExceptionHandler can answer with the contract's FORBIDDEN body
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountForbiddenException extends RuntimeException {
    public AccountForbiddenException() {
        super("You do not have access to this account.");
    }
}
