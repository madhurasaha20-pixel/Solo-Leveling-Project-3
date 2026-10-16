package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // the principal name is the logged-in user's id (the token's subject, see UserIdJwtAuthenticationConverter)
    @PatchMapping("/{accountId}")
    ResponseEntity<AccountResponse> updateAccount(@PathVariable String accountId,
                                                  @Valid @RequestBody UpdateAccountRequest request,
                                                  Principal principal) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, UUID.fromString(principal.getName()), request));
    }
}
