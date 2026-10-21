package com.solo_leveling.bank_of_cli_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Body of DELETE /users/me. The user re-enters their password to confirm. */
public record DeleteUserRequest(
        @NotBlank(message = "Password is required.")
        String password
) {
}