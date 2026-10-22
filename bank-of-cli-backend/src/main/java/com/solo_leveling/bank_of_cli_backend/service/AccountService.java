package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponse getMyAccount(UUID ownerId);
    List<AccountResponse> getMyAccounts(UUID ownerId);

    AccountResponse createAccount(UUID ownerId, CreateAccountRequest request);

    AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request);
}
