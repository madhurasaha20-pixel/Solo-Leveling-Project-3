package com.solo_leveling.bank_of_cli_backend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Incorrect email or password.");
    }
}
