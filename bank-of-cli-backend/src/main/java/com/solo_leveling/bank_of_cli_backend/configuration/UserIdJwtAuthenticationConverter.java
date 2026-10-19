package com.solo_leveling.bank_of_cli_backend.configuration;

import com.solo_leveling.bank_of_cli_backend.repository.BankUserRepository;
import com.solo_leveling.bank_of_cli_backend.repository.UserRoleRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Turns a verified token into an authentication. The token only carries the user id (sub),
 * so the user's existence and roles are loaded from the database on every request.
 */
@Component
class UserIdJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final BankUserRepository users;
    private final UserRoleRepository userRoles;

    UserIdJwtAuthenticationConverter(BankUserRepository users, UserRoleRepository userRoles) {
        this.users = users;
        this.userRoles = userRoles;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = parseSubject(jwt.getSubject());
        if (!users.existsById(userId)) {
            throw new InvalidBearerTokenException("Token subject does not exist");
        }

        List<GrantedAuthority> authorities = userRoles.findByUserId(userId).stream()
                .<GrantedAuthority>map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getName()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }

    private static UUID parseSubject(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token subject is not a user id");
        }
    }
}
