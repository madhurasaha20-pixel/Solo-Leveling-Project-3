package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.CreateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.CreateAccountResponse;

import java.util.UUID;

public interface AccountService {
    AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request);

    CreateAccountResponse addNewAccount(
            CreateAccountRequest request,
            UUID ownerId
    );
}
