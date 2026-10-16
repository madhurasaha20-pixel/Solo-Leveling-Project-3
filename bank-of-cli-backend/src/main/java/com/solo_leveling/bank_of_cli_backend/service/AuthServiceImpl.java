package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.LoginRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.RegisterRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.AuthResponse;
import com.solo_leveling.bank_of_cli_backend.dto.response.UserResponse;
import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.entity.Role;
import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import com.solo_leveling.bank_of_cli_backend.entity.UserRole;
import com.solo_leveling.bank_of_cli_backend.exception.EmailTakenException;
import com.solo_leveling.bank_of_cli_backend.exception.InvalidCredentialsException;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.RoleRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {
    static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    private final BankUserRepository users;
    private final UserCredentialRepository credentials;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;

    public AuthServiceImpl(BankUserRepository users,
                           UserCredentialRepository credentials,
                           RoleRepository roles,
                           UserRoleRepository userRoles,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenService tokenService) {
        this.users = users;
        this.credentials = credentials;
        this.roles = roles;
        this.userRoles = userRoles;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }

        Role role = roles.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Missing role " + DEFAULT_ROLE));

        BankUser user = new BankUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        try {
            // flush now so a concurrent duplicate surfaces here, not at commit
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailTakenException();
        }

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credentials.save(credential);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoles.save(userRole);

        return new AuthResponse(tokenService.issue(user.getId()), UserResponse.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException e) {
            // unknown email and wrong password are deliberately indistinguishable
            throw new InvalidCredentialsException();
        }

        BankUser user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        return new AuthResponse(tokenService.issue(user.getId()), UserResponse.from(user));
    }
}
