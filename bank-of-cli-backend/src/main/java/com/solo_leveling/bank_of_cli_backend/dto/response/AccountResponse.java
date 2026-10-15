package com.solo_leveling.bank_of_cli_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.solo_leveling.bank_of_cli_backend.entity.Account;

import java.math.BigDecimal;

/** An Account, as defined in the frontend API contract. nickname is left out when not set. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        String id,
        String userId,
        String type,
        String nickname,
        BigDecimal balance,
        String currency
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUser().getId().toString(),
                account.getType(),
                account.getNickname(),
                account.getBalance(),
                account.getCurrency());
    }
}
