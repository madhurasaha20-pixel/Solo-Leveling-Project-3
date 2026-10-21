package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    /** All of the user's accounts, oldest first. Empty when they haven't opened one yet. */
    List<AccountResponse> listAccounts(UUID ownerId);

    /** The user's main account: their first checking account, else their first account. */
    AccountResponse mainAccount(UUID ownerId);

    AccountResponse createAccount(UUID ownerId, CreateAccountRequest request);

    AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request);
}
