package ru.galtor85.household_store.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Role;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:my-very-secret-key-with-at-least-32-characters-1234567890}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-validity:3600000}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity:86400000}")
    private long refreshTokenValidity;

    // Создаем AES ключ для шифрования (A256GCM требует AES-256)
    private SecretKey getEncryptionKey() {
        try {
            String secret = jwtSecret;

            // Для A256GCM нужен ключ 256 бит (32 байта)
            byte[] keyBytes;

            if (secret.length() >= 32) {
                keyBytes = secret.substring(0, 32).getBytes(StandardCharsets.UTF_8);
            } else {
                // Дополняем до 32 байт
                keyBytes = new byte[32];
                byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
                System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
            }

            log.info("Creating AES-256 key from secret, key length: {} bytes", keyBytes.length);

            // Создаем AES ключ
            return new SecretKeySpec(keyBytes, "AES");

        } catch (Exception e) {
            log.error("Failed to create encryption key: {}", e.getMessage());
            throw new RuntimeException("Encryption key initialization failed", e);
        }
    }

    public String createToken(String email, Role role) {
        log.info("Creating encrypted JWT token for: {}, role: {}", email, role);

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + accessTokenValidity);

            // Создаем claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role.name());
            claims.put("type", "access");

            // Шифруем токен с использованием AES-256-GCM
            return Jwts.builder()
                    .claims(claims)
                    .subject(email)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error("Error creating encrypted JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create encrypted JWT token", e);
        }
    }

    public String createRefreshToken(String email) {
        log.info("Creating encrypted refresh token for: {}", email);

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + refreshTokenValidity);

            // Создаем claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("type", "refresh");

            // Шифруем токен
            return Jwts.builder()
                    .claims(claims)
                    .subject(email)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error("Error creating refresh token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create refresh token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            // Пытаемся расшифровать токен
            Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Decryption failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            log.error("Cannot extract username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    public String getRoleFromToken(String token) {
        try {
            return Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .get("role", String.class);
        } catch (Exception e) {
            log.error("Cannot extract role from token: {}", e.getMessage());
            return null;
        }
    }

    public Long getValidity() {
        return accessTokenValidity / 1000; // в секундах
    }
}