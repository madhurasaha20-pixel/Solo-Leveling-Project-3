package com.solo_leveling.bank_of_cli_backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(configurer ->
                configurer
                        .requestMatchers( "/auth/*").permitAll()
                        .anyRequest().authenticated()

        );
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
