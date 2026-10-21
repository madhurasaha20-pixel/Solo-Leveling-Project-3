package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.LoginRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.RegisterRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AuthResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.UserResponse;
import com.solo_leveling.bank_of_cli_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    ResponseEntity<UserResponse> profile(Principal principal) {
        return ResponseEntity.ok(authService.getProfile(UUID.fromString(principal.getName())));
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // tokens are stateless and have no expiry yet, so there is nothing to revoke server-side;
    // the client discards its token. Requires a valid token like any protected endpoint.
    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
