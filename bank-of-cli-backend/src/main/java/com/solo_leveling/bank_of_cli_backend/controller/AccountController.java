package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.CreateAccountResponse;
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

    //controller method for creating a new account, the format for request can be found inside the CreateAccountRequest
    //file in the dto request folder
    /*
    @PostMapping("/newAccount")
    public CreateAccountResponse addNewAccount(@RequestBody CreateAccountRequest request){
        return accountService.addNewAccount(request);
    } */

    @PostMapping("/newAccount")
    public CreateAccountResponse addNewAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Principal principal) {

        return accountService.addNewAccount(
                request,
                principal.getName()
        );
    }
}
