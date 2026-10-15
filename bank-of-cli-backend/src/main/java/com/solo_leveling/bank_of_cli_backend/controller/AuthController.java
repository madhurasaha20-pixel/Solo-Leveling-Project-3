package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.RegisterRequest;
import com.solo_leveling.bank_of_cli_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // HTTP Basic authentication happens in the security filter chain; reaching here means it succeeded
    @PostMapping("/login")
    ResponseEntity<String> login() {
        return ResponseEntity.ok("Login successful");
    }
}
