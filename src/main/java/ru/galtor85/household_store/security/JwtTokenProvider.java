package ru.galtor85.household_store.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.UnauthorizedException;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.service.MessageService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final MessageService messageService;

    @Value("${app.jwt.secret:my-very-secret-key-with-at-least-32-characters-1234567890}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-validity:3600000}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity:86400000}")
    private long refreshTokenValidity;

    public JwtTokenProvider(MessageService messageService) {
        this.messageService = messageService;
    }

    private SecretKey getEncryptionKey() {
        try {
            String secret = jwtSecret;
            byte[] keyBytes;

            if (secret.length() >= 32) {
                keyBytes = secret.substring(0, 32).getBytes(StandardCharsets.UTF_8);
            } else {
                keyBytes = new byte[32];
                byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
                System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
            }

            log.info(messageService.get("jwt-token-provider.jwt.key.created", keyBytes.length));
            return new SecretKeySpec(keyBytes, "AES");

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.key.error", e.getMessage()), e);
            throw new RuntimeException("Encryption key initialization failed", e);
        }
    }

    public String createToken(String identify, Role role) {
        log.info(messageService.get("jwt-token-provider.jwt.token.creating", identify, role));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + accessTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role.name());
            claims.put("identify", identify);
            claims.put("type", "access");

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.token.create.error", e.getMessage()), e);
            throw new RuntimeException("Failed to create encrypted JWT token", e);
        }
    }

    public String createRefreshToken(String identify) {
        log.info(messageService.get("jwt-token-provider.jwt.refresh.creating", identify));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + refreshTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put("identify", identify);
            claims.put("type", "refresh");

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.refresh.create.error", e.getMessage()), e);
            throw new RuntimeException("Failed to create refresh token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn(messageService.get("jwt-token-provider.jwt.error.expired", e.getMessage()));
            throw new UnauthorizedException(messageService.get("jwt-token-provider.jwt.error.expired", e.getMessage()));
        } catch (UnsupportedJwtException e) {
            log.warn(messageService.get("jwt-token-provider.jwt.error.unsupported", e.getMessage()));
        } catch (MalformedJwtException e) {
            log.warn(messageService.get("jwt-token-provider.jwt.error.malformed", e.getMessage()));
        } catch (SecurityException e) {
            log.warn(messageService.get("jwt-token-provider.jwt.error.security", e.getMessage()));
        } catch (Exception e) {
            log.warn(messageService.get("jwt-token-provider.jwt.error.invalid", e.getMessage()));
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        try {
            String username = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getSubject();

            log.debug(messageService.get("jwt-token-provider.jwt.username.extracted", username));
            return username;

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.username.error", e.getMessage()));
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    public String getIdentifierFromToken(String token) {
        try {
            String identifier = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .get("identify", String.class);

            log.debug(messageService.get("jwt-token-provider.jwt.identifier.extracted", identifier));
            return identifier;

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.identifier.error", e.getMessage()));
            throw new RuntimeException("Invalid or expired token", e); //TODO
        }
    }

    public String getRoleFromToken(String token) {
        try {
            String role = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .get("role", String.class);

            log.debug(messageService.get("jwt-token-provider.jwt.role.extracted", role));
            return role;

        } catch (Exception e) {
            log.error(messageService.get("jwt-token-provider.jwt.role.error", e.getMessage()));
            return null;
        }
    }

    public Long getValidity() {
        return accessTokenValidity / 1000;
    }
}