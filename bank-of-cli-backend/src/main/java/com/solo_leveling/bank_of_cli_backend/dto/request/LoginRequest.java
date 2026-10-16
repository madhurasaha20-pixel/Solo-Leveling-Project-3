package com.solo_leveling.bank_of_cli_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required.")
        String email,

        @NotBlank(message = "Password is required.")
        String password
) {
}
