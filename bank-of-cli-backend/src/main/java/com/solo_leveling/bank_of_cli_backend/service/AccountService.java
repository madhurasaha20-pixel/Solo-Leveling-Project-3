package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;

import java.util.UUID;

public interface AccountService {
    AccountResponse updateAccount(String accountId, UUID ownerId, UpdateAccountRequest request);
}
