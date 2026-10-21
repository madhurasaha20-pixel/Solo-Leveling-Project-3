package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// the principal name is the logged-in user's id (the token's subject, see UserIdJwtAuthenticationConverter)
@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    ResponseEntity<List<AccountResponse>> listAccounts(Principal principal) {
        return ResponseEntity.ok(accountService.listAccounts(ownerId(principal)));
    }

    @GetMapping("/me")
    ResponseEntity<AccountResponse> mainAccount(Principal principal) {
        return ResponseEntity.ok(accountService.mainAccount(ownerId(principal)));
    }

    @PostMapping
    ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                  Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(ownerId(principal), request));
    }

    @PatchMapping("/{accountId}")
    ResponseEntity<AccountResponse> updateAccount(@PathVariable String accountId,
                                                  @Valid @RequestBody UpdateAccountRequest request,
                                                  Principal principal) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, ownerId(principal), request));
    }

    private static UUID ownerId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
