package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
}
