package com.solo_leveling.bank_of_cli_backend.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes 401/403 produced inside the security filter chain in the API's error body shape. */
final class RestSecurityErrorHandlers {
    private RestSecurityErrorHandlers() {
    }

    static final AuthenticationEntryPoint ENTRY_POINT = (request, response, exception) -> {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Your session has expired. Please log in again.");
    };

    static final AccessDeniedHandler ACCESS_DENIED_HANDLER = (request, response, exception) ->
            write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this resource.");

    // messages are fixed strings, so no JSON escaping is needed
    private static void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\"}".formatted(status.value(), code, message));
    }
}
