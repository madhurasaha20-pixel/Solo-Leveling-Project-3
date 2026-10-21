package com.solo_leveling.bank_of_cli_backend.exception;

/**
 * The logged-in user typed the wrong password while confirming a sensitive action
 * (e.g. deleting their profile). Answered as 400 VALIDATION_ERROR on the password field,
 * not 401, so the frontend doesn't mistake it for an expired session.
 */
public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException() {
        super("Incorrect password.");
    }
}