package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.service.WireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/wire")
public class WireController {

    private final WireService wireService;

    @Autowired
    public WireController(WireService wireService) {
        this.wireService = wireService;
    }

    @PostMapping("/{fromAccountId}/{toAccountId}/{amount}")
    public ResponseEntity<Void> wireTransfer(@PathVariable String fromAccountId,
                                             @PathVariable String toAccountId,
                                             @PathVariable BigDecimal amount,
                                             Authentication authentication) {


        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        String userId = authentication.getName();


        if (fromAccountId.isBlank() || toAccountId.isBlank()) {
            throw new IllegalArgumentException("Both account ids are required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places.");
        }


        wireService.executeWireTransfer(userId, fromAccountId, toAccountId, amount);
        return ResponseEntity.ok().build();
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}