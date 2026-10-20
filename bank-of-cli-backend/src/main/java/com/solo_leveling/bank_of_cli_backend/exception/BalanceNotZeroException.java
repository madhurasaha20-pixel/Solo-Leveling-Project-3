package com.solo_leveling.bank_of_cli_backend.exception;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * The user tried to delete their profile while one of their accounts still holds money.
 * Answered as 409 BALANCE_NOT_ZERO; nothing is deleted.
 */
public class BalanceNotZeroException extends RuntimeException {
    /** @param usdTotal the remaining total when every funded account is in USD, otherwise null */
    public BalanceNotZeroException(BigDecimal usdTotal) {
        super(usdTotal != null
                ? String.format(Locale.US, "You still have $%,.2f in your accounts. "
                + "Withdraw or transfer it first, so every account is at $0.", usdTotal)
                : "Your accounts still have money in them. "
                + "Withdraw or transfer it first, so every account is at $0.");
    }
}