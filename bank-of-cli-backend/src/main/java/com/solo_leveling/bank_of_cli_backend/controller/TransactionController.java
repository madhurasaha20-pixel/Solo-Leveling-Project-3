package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.DepositRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.TransactionResponse;
import com.solo_leveling.bank_of_cli_backend.service.TransactionService;
import com.solo_leveling.bank_of_cli_backend.dto.request.WithdrawRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String accountId,
            @Valid @RequestBody DepositRequest request,
            Principal principal
    ) {
        TransactionResponse response = TransactionResponse.from(
                transactionService.deposit(
                        accountId,
                        UUID.fromString(principal.getName()),
                        request
                )
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody WithdrawRequest request,
            Principal principal
    ) {
        TransactionResponse response = TransactionResponse.from(
                transactionService.withdraw(
                        accountId,
                        UUID.fromString(principal.getName()),
                        request
                )
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable String accountId,
            @Valid @RequestBody TransferRequest request,
            Principal principal
    ) {
        TransactionResponse response = TransactionResponse.from(
                transactionService.transfer(
                        accountId,
                        UUID.fromString(principal.getName()),
                        request
                )
        );

        return ResponseEntity.ok(response);
    }
}