package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.CreateAccountResponse;

import java.util.UUID;
import java.util.List;

public interface AccountService {
    AccountResponse getMyAccount(UUID ownerId);
    List<AccountResponse> getMyAccounts(UUID ownerId);

    AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request);

    CreateAccountResponse addNewAccount(
            CreateAccountRequest request,
            UUID ownerId
    );
}
