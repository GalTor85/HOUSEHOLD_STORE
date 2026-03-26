package ru.galtor85.household_store.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.MessageService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
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

            log.info(messageService.get("jwt.log.key.created", keyBytes.length));
            return new SecretKeySpec(keyBytes, "AES");

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.key.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.key.error", e.getMessage()), e);
        }
    }

    public String createToken(SecurityUser securityUser, User user) {
        String identify = user.getAuthenticationId();
        log.info(messageService.get("jwt.log.token.creating", identify, securityUser.getRole()));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + accessTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", securityUser.getRole().name());
            claims.put("identify", identify);
            claims.put("userId", securityUser.getUserId());
            claims.put("type", "access");

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.token.create.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.token.create.error", e.getMessage()), e);
        }
    }

    public String createRefreshToken(SecurityUser securityUser, User user) {
        String identify = user.getAuthenticationId();
        log.info(messageService.get("jwt.log.refresh.creating", identify));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + refreshTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put("identify", identify);
            claims.put("userId", securityUser.getUserId());
            claims.put("type", "refresh");

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.refresh.create.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.refresh.create.error", e.getMessage()), e);
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
            log.warn(messageService.get("jwt.log.error.expired", e.getMessage()));
            throw new TokenExpiredException();
        } catch (UnsupportedJwtException e) {
            log.warn(messageService.get("jwt.log.error.unsupported", e.getMessage()));
            throw new TokenUnsupportedException();
        } catch (MalformedJwtException e) {
            log.warn(messageService.get("jwt.log.error.malformed", e.getMessage()));
            throw new TokenMalformedException();
        } catch (SecurityException e) {
            log.warn(messageService.get("jwt.log.error.security", e.getMessage()));
            throw new TokenSecurityException();
        } catch (Exception e) {
            log.warn(messageService.get("jwt.log.error.invalid", e.getMessage()));
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            String username = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getSubject();

            log.debug(messageService.get("jwt.log.username.extracted", username));
            return username;

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.username.error", e.getMessage()));
            throw new TokenMalformedException();
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Long userId = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .get("userId", Long.class);

            log.debug(messageService.get("jwt.log.userid.extracted", userId));
            return userId;

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.userid.error", e.getMessage()));
            throw new TokenMalformedException();
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

            log.debug(messageService.get("jwt.log.role.extracted", role));
            return role;

        } catch (Exception e) {
            log.error(messageService.get("jwt.log.role.error", e.getMessage()));
            return null;
        }
    }

    public LocalDateTime getExpirationDateFromToken(String token) {
        try {
            Date expiration = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("Failed to get expiration date: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    public Long getValidity() {
        return accessTokenValidity / 1000;
    }
}