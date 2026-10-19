package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.LoginRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.RegisterRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AuthResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    UserResponse getProfile(UUID userId);

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
