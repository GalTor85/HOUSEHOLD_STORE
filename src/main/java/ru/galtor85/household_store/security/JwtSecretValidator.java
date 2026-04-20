package ru.galtor85.household_store.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JwtSecretValidator {

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "my-very-secret-key-with-at-least-32-characters-1234567890",
            "changeit",
            "secret"
    );

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    void validate() {
        if (FORBIDDEN_SECRETS.contains(jwtSecret)) {
            throw new IllegalStateException("JWT secret must not be a default value!");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters!");
        }
    }
}