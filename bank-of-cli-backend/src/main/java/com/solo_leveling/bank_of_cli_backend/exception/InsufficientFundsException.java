package com.solo_leveling.bank_of_cli_backend.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Insufficient funds. Please try again.");
    }
}