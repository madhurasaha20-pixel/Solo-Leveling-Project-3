package com.solo_leveling.bank_of_cli_backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WireRequest(

        @NotBlank(message = "Source account cannot be empty/blank.")
        @Size(max = 20, message = "Source account id cannot exceed 20 characters.")
        @Pattern(regexp = "^ACC-\\d{4,}$", message = "Source account must look like ACC-1001.")
        String fromAccountId,

        // WIRE: recipient email replaces toAccountId
        @NotBlank(message = "Recipient email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email is too long.")
        String toEmail,

        @NotNull(message = "You must transfer an amount.")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01.")
        @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000.00 per wire.")
        @Digits(integer = 7, fraction = 2, message = "Amount allows at most 7 whole digits and 2 decimal places.")
        BigDecimal amount
) {}