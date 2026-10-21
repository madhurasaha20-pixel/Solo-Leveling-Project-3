package com.solo_leveling.bank_of_cli_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /accounts. nickname is optional; the currency is always USD (see the API contract). */
public record CreateAccountRequest(
        @NotBlank(message = "Account type is required.")
        @Pattern(regexp = "checking|savings", message = "Account type must be checking or savings.")
        String type,

        @Size(max = 30, message = "Nickname must be 30 characters or fewer.")
        String nickname
) {
}
