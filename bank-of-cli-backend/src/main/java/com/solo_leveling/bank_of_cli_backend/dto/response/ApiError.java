package com.solo_leveling.bank_of_cli_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of every failed request, as defined in the frontend API contract. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String code,
        String message,
        String field
) {
}
