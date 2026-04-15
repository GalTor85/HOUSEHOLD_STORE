package ru.galtor85.household_store.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.auth.TokenExpiredException;
import ru.galtor85.household_store.advice.exception.auth.TokenMalformedException;
import ru.galtor85.household_store.advice.exception.auth.TokenSecurityException;
import ru.galtor85.household_store.advice.exception.auth.TokenUnsupportedException;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Provider for JWT token operations including creation, validation, and parsing.
 *
 * <p>Handles both access tokens and refresh tokens using AES-256-GCM encryption.
 * Tokens contain user identification, role, and expiration information.</p>
 *
 * <p><b>Security features:</b>
 * <ul>
 *   <li>Tokens are encrypted (JWE) rather than just signed (JWS)</li>
 *   <li>Access and refresh tokens have different validity periods</li>
 *   <li>Token validation throws specific exceptions for different failure modes</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String JWT_ALGORITHM = "AES";
    private static final long MILLIS_PER_SECOND = 1000;

    private final MessageService messageService;
    private final LogMessageService logMsg;

    @Value("${app.jwt.secret:my-very-secret-key-with-at-least-32-characters-1234567890}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-validity:3600000}")
    private long accessTokenValidity;

    @Value("${app.jwt.refresh-token-validity:86400000}")
    private long refreshTokenValidity;

    @Value("${app.jwt.key-length:32}")
    private int jwtKeyLength;

    public JwtTokenProvider(MessageService messageService, LogMessageService logMsg) {
        this.messageService = messageService;
        this.logMsg = logMsg;
    }

    /**
     * Creates an access token for the authenticated user.
     *
     * @param securityUser the security user entity
     * @param user the domain user entity
     * @return JWT access token string
     */
    public String createToken(SecurityUser securityUser, User user) {
        String identify = user.getAuthenticationId();
        log.info(logMsg.get("jwt.log.token.creating", identify, securityUser.getRole()));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + accessTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put(JWT_CLAIM_ROLE, securityUser.getRole().name());
            claims.put(JWT_CLAIM_IDENTIFY, identify);
            claims.put(JWT_CLAIM_USER_ID, securityUser.getUserId());
            claims.put(JWT_CLAIM_TYPE, JWT_TOKEN_TYPE_ACCESS);

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(logMsg.get("jwt.log.token.create.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.token.create.error", e.getMessage()), e);
        }
    }

    /**
     * Creates a refresh token for the authenticated user.
     *
     * @param securityUser the security user entity
     * @param user the domain user entity
     * @return JWT refresh token string
     */
    public String createRefreshToken(SecurityUser securityUser, User user) {
        String identify = user.getAuthenticationId();
        log.info(logMsg.get("jwt.log.refresh.creating", identify));

        try {
            Date now = new Date();
            Date validity = new Date(now.getTime() + refreshTokenValidity);

            Map<String, Object> claims = new HashMap<>();
            claims.put(JWT_CLAIM_IDENTIFY, identify);
            claims.put(JWT_CLAIM_USER_ID, securityUser.getUserId());
            claims.put(JWT_CLAIM_TYPE, JWT_TOKEN_TYPE_REFRESH);

            return Jwts.builder()
                    .claims(claims)
                    .subject(identify)
                    .issuedAt(now)
                    .expiration(validity)
                    .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
                    .compact();

        } catch (Exception e) {
            log.error(logMsg.get("jwt.log.refresh.create.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.refresh.create.error", e.getMessage()), e);
        }
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if token is valid
     * @throws TokenExpiredException if token has expired
     * @throws TokenUnsupportedException if token type is unsupported
     * @throws TokenMalformedException if token is malformed
     * @throws TokenSecurityException if token signature/encryption is invalid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn(logMsg.get("jwt.log.error.expired", e.getMessage()));
            throw new TokenExpiredException();
        } catch (UnsupportedJwtException e) {
            log.warn(logMsg.get("jwt.log.error.unsupported", e.getMessage()));
            throw new TokenUnsupportedException();
        } catch (MalformedJwtException e) {
            log.warn(logMsg.get("jwt.log.error.malformed", e.getMessage()));
            throw new TokenMalformedException();
        } catch (SecurityException e) {
            log.warn(logMsg.get("jwt.log.error.security", e.getMessage()));
            throw new TokenSecurityException();
        } catch (Exception e) {
            log.warn(logMsg.get("jwt.log.error.invalid", e.getMessage()));
            return false;
        }
    }

    /**
     * Extracts the username (identifier) from a token.
     *
     * @param token the JWT token
     * @return the username/identifier
     * @throws TokenMalformedException if token cannot be parsed
     */
    public String getUsernameFromToken(String token) {
        try {
            String username = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .getSubject();

            log.debug(logMsg.get("jwt.log.username.extracted", username));
            return username;

        } catch (Exception e) {
            log.error(logMsg.get("jwt.log.username.error", e.getMessage()));
            throw new TokenMalformedException();
        }
    }

    /**
     * Extracts the user ID from a token.
     *
     * @param token the JWT token
     * @return the user ID
     * @throws TokenMalformedException if token cannot be parsed
     */
    public Long getUserIdFromToken(String token) {
        try {
            Long userId = Jwts.parser()
                    .decryptWith(getEncryptionKey())
                    .build()
                    .parseEncryptedClaims(token)
                    .getPayload()
                    .get(JWT_CLAIM_USER_ID, Long.class);

            log.debug(logMsg.get("jwt.log.userid.extracted", userId));
            return userId;

        } catch (Exception e) {
            log.error(logMsg.get("jwt.log.userid.error", e.getMessage()));
            throw new TokenMalformedException();
        }
    }

    /**
     * Extracts the expiration date from a token.
     *
     * @param token the JWT token
     * @return the expiration date as LocalDateTime
     */
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

    /**
     * Gets the access token validity period in seconds.
     *
     * @return validity in seconds
     */
    public Long getValidity() {
        return accessTokenValidity / MILLIS_PER_SECOND;
    }

    private SecretKey getEncryptionKey() {
        try {
            String secret = jwtSecret;
            byte[] keyBytes;

            if (secret.length() >= jwtKeyLength) {
                keyBytes = secret.substring(0, jwtKeyLength).getBytes(StandardCharsets.UTF_8);
            } else {
                keyBytes = new byte[jwtKeyLength];
                byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
                System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, jwtKeyLength));
            }

            log.info(logMsg.get("jwt.log.key.created", keyBytes.length));
            return new SecretKeySpec(keyBytes, JWT_ALGORITHM);

        } catch (Exception e) {
            log.error(logMsg.get("jwt.log.key.error", e.getMessage()), e);
            throw new RuntimeException(messageService.get("jwt.log.key.error", e.getMessage()), e);
        }
    }
}