package com.solo_leveling.bank_of_cli_backend.dto.response;

/** Returned by both login and register. */
public record AuthResponse(
        String token,
        UserResponse user
) {
}
