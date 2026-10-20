package com.solo_leveling.bank_of_cli_backend.dto.request;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAccountRequest {

    private String accountType;
    private String nickname;
    private String currency;

    public CreateAccountRequest() {
    }

    @JsonProperty("accountType")
    public String getAccountTypeForNewAccount() {
        return accountType;
    }

    @JsonProperty("nickname")
    public String getNicknameForNewAccount() {
        return nickname;
    }

    @JsonProperty("currency")
    public String getCurrencyForNewAccount() {
        return currency;
    }

    @JsonProperty("accountType")
    public void setAccountTypeForNewAccount(String accountType) {
        this.accountType = accountType;
    }

    @JsonProperty("nickname")
    public void setNicknameForNewAccount(String nickname) {
        this.nickname = nickname;
    }

    @JsonProperty("currency")
    public void setCurrencyForNewAccount(String currency) {
        this.currency = currency;
    }
}