package com.solo_leveling.bank_of_cli_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailTakenException extends RuntimeException {
    public EmailTakenException() {
        super("An account with that email already exists.");
    }
}
