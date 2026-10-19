package com.solo_leveling.bank_of_cli_backend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of PATCH /accounts/{accountId}. Leave a field out (or null) to keep it as is.
 * A blank nickname clears it.
 */
public record UpdateAccountRequest(
        @Size(max = 30, message = "Nickname must be 30 characters or fewer.")
        String nickname,

        @Pattern(regexp = "checking|savings", message = "Account type must be checking or savings.")
        String type
) {
}
