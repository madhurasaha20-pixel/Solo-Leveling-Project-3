package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/hello-world")
    ResponseEntity<String> getHelloWorld() {
        return ResponseEntity.ok("Hello World");
    }
}
