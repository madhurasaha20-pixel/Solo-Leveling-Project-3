package com.solo_leveling.bank_of_cli_backend.dto.response;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt
) {
    public static UserResponse from(BankUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
