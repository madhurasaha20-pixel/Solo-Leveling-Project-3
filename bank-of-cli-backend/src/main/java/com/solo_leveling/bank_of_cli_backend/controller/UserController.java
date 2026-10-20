package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.dto.request.DeleteUserRequest;
import com.solo_leveling.bank_of_cli_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // DELETE /api/users/me: deletes the logged-in user (the token's subject) and everything they own
    @DeleteMapping("/me")
    ResponseEntity<Void> deleteMe(@Valid @RequestBody DeleteUserRequest request, Principal principal) {
        userService.deleteUser(UUID.fromString(principal.getName()), request);
        return ResponseEntity.noContent().build();
    }
}