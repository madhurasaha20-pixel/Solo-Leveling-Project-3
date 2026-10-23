
package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.WireRequest;
import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.service.TransferProcessor;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wire")
public class WireController {

    private final TransferProcessor processor;
    private final BankUserRepository bankUserRepository;
    private final AccountRepository accountRepository;

    public WireController(
            @Qualifier("wireTransferProcessor") TransferProcessor processor,
            BankUserRepository bankUserRepository,
            AccountRepository accountRepository) {

        this.processor = processor;
        this.bankUserRepository = bankUserRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<Void> wireTransfer(
            @Valid @RequestBody WireRequest request,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Authentication required.");
        }

        UUID ownerId = UUID.fromString(authentication.getName());

        BankUser recipient = bankUserRepository
                .findByEmail(request.toEmail().trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Recipient not found."));

        List<Account> recipientAccounts =
                accountRepository.findAllByUser_IdOrderByCreatedAtAscIdAsc(
                        recipient.getId());

        Account destination = recipientAccounts.stream()
                .filter(a -> "checking".equalsIgnoreCase(
                        a.getAccountType()))
                .findFirst()
                .or(() -> recipientAccounts.stream().findFirst())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Recipient has no account."));

        processor.transfer(
                request.fromAccountId(),
                ownerId,
                destination.getId(),
                request.amount());

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(
            IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict(
            IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> forbidden(
            AccessDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(e.getMessage());
    }
}
