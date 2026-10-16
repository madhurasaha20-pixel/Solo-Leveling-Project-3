package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // principal is the logged-in user's email (see DatabaseUserDetailsService)
    @PatchMapping("/{accountId}")
    ResponseEntity<AccountResponse> updateAccount(@PathVariable String accountId,
                                                  @Valid @RequestBody UpdateAccountRequest request,
                                                  Principal principal) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, principal.getName(), request));
    }
}
