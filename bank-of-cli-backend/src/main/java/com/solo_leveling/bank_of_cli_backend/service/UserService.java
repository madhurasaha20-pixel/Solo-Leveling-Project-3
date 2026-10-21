package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.DeleteUserRequest;

import java.util.UUID;

public interface UserService {
    /**
     * Permanently deletes the user with all of their accounts, transactions, credentials and roles.
     * Refused with BalanceNotZeroException while any of their accounts holds money.
     * All or nothing: if any step fails, nothing is deleted.
     */
    void deleteUser(UUID userId, DeleteUserRequest request);
}