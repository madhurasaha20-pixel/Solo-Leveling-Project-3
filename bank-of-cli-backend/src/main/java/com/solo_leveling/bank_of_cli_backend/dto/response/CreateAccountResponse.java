package com.solo_leveling.bank_of_cli_backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;

public class CreateAccountResponse {
    private boolean success;
    private String message;

    public CreateAccountResponse(boolean success, String message){
        this.success = success;
        this.message = message;
    }

    public CreateAccountResponse(){}

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}