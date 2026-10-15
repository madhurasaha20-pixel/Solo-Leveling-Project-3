package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserCredentialRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Loads users for authentication. The "username" is the user's email. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final BankUserRepository users;
    private final UserCredentialRepository credentials;
    private final UserRoleRepository userRoles;

    public DatabaseUserDetailsService(BankUserRepository users,
                                      UserCredentialRepository credentials,
                                      UserRoleRepository userRoles) {
        this.users = users;
        this.credentials = credentials;
        this.userRoles = userRoles;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        BankUser user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        UserCredential credential = credentials.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        List<SimpleGrantedAuthority> authorities = userRoles.findByUserId(user.getId()).stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getName()))
                .toList();

        return User.withUsername(user.getEmail())
                .password(credential.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
