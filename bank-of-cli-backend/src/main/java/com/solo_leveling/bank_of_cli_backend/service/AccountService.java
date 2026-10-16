package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.UpdateAccountRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AccountResponse;

public interface AccountService {
    AccountResponse updateAccount(String accountId, String ownerEmail, UpdateAccountRequest request);
}
