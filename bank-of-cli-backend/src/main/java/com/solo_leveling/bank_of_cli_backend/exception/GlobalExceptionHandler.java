package com.solo_leveling.bank_of_cli_backend.exception;

import com.solo_leveling.bank_of_cli_backend.dto.response.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        var fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid request.";
        String field = fieldError != null ? fieldError.getField() : null;
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, field);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request body is missing or malformed.", null);
    }

    @ExceptionHandler(EmailTakenException.class)
    ResponseEntity<ApiError> handleEmailTaken(EmailTakenException e) {
        return build(HttpStatus.CONFLICT, "EMAIL_TAKEN", e.getMessage(), "email");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException e) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage(), null);
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    ResponseEntity<ApiError> handleIncorrectPassword(IncorrectPasswordException e) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage(), "password");
    }

    @ExceptionHandler(BalanceNotZeroException.class)
    ResponseEntity<ApiError> handleBalanceNotZero(BalanceNotZeroException e) {
        return build(HttpStatus.CONFLICT, "BALANCE_NOT_ZERO", e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found.", null);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", e.getMessage(), null);
    }

    @ExceptionHandler(AccountForbiddenException.class)
    ResponseEntity<ApiError> handleAccountForbidden(AccountForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception e) throws Exception {
        // let Spring Security's own handling (401/403) deal with these
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        log.error("Unhandled exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN", "Something went wrong. Please try again.", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, String field) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), code, message, field));
    }
}
