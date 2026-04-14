package ru.galtor85.household_store.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.auth.*;
import ru.galtor85.household_store.dto.request.auth.LoginFormRequest;
import ru.galtor85.household_store.dto.request.auth.UserCreateRequest;
import ru.galtor85.household_store.dto.response.auth.AuthResponse;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.security.BlacklistedToken;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.mapper.user.UserMapper;
import ru.galtor85.household_store.mapper.user.UserToEntity;
import ru.galtor85.household_store.repository.security.BlacklistedTokenRepository;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.resolver.UserIdentifierResolver;
import ru.galtor85.household_store.security.JwtTokenProvider;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.validator.auth.AuthenticationValidator;

import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.TOKEN_TYPE;

/**
 * Service for handling authentication and authorization operations.
 *
 * <p>This service provides core authentication functionality including:
 * user registration, login, logout, token validation, and token refresh.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>User registration with automatic role assignment</li>
 *   <li>Authentication using email/phone + password</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Token blacklisting for logout</li>
 *   <li>Refresh token rotation</li>
 * </ul>
 *
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>Passwords are hashed using BCrypt</li>
 *   <li>Tokens are blacklisted on logout to prevent reuse</li>
 *   <li>Expired tokens are automatically cleaned up</li>
 *   <li>Account deactivation check during login and refresh</li>
 * </ul>
 *
 * @author G@LTor85
 * @see JwtTokenProvider
 * @see AuthenticationManager
 * @see BlacklistedTokenRepository
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /**
     * Maximum length of token preview in logs (prevents full token exposure).
     */
    private static final int TOKEN_PREVIEW_MAX_LENGTH = 20;

    /**
     * Ellipsis for truncated token display.
     */
    private static final String TOKEN_ELLIPSIS = "...";

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserSearchService userSearchService;
    private final UserToEntity userToEntity;
    private final UserMapper userMapper;
    private final LogMessageService logMsg;
    private final UserIdentifierResolver userIdentifierResolver;
    private final SecurityUserRepository securityUserRepository;
    private final AuthenticationValidator authenticationValidator;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    // =========================================================================
    // REGISTRATION
    // =========================================================================

    /**
     * Registers a new user in the system.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Builds user entity from request</li>
     *   <li>Creates user with hashed password</li>
     *   <li>Loads SecurityUser from repository</li>
     *   <li>Generates JWT tokens</li>
     *   <li>Returns authentication response</li>
     * </ol>
     * </p>
     *
     * @param request user creation request containing email, password, etc.
     * @return authentication response with access token and user details
     */
    @Transactional
    public AuthResponse register(UserCreateRequest request) {
        log.debug(logMsg.get("auth.log.register.attempt", request.getEmail()));

        User user = userToEntity.build(request,null);
        User registeredUser = userService.register(user, request.getPassword());

        SecurityUser securityUser = securityUserRepository.findById(registeredUser.getId())
                .orElseThrow(() -> {
                    log.error(logMsg.get("auth.log.security.user.not.found", registeredUser.getId()));
                    return new SecurityUserNotFoundException(registeredUser.getId());
                });

        User userForResponse = userSearchService.getUserById(registeredUser.getId());

        log.info(logMsg.get("auth.log.user.registered", registeredUser.getEmail()));

        return buildAuthResponse(securityUser, userForResponse);
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Authenticates a user with email/phone and password.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Resolves identifier (email or phone) from request</li>
     *   <li>Attempts authentication via AuthenticationManager</li>
     *   <li>Sets authentication in SecurityContext</li>
     *   <li>Checks if account is enabled</li>
     *   <li>Generates JWT tokens</li>
     * </ol>
     * </p>
     *
     * @param request login request containing identifier and password
     * @return authentication response with access token and user details
     * @throws InvalidCredentialsException      if credentials are invalid
     * @throws AccountDeactivatedException      if account is disabled
     */
    public AuthResponse login(LoginFormRequest request) {
        String identify = userIdentifierResolver.resolve(request);
        log.debug(logMsg.get("auth.log.login.attempt", identify));

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identify, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            assert securityUser != null;
            User user = userSearchService.getUserById(securityUser.getUserId());

            if (!securityUser.isEnabled()) {
                log.warn(logMsg.get("auth.log.login.deactivated", identify));
                throw new AccountDeactivatedException(securityUser.getUserId());
            }

            log.info(logMsg.get("auth.log.login.success",
                    user.getAuthenticationId(), securityUser.getId()));

            return buildAuthResponse(securityUser, user);

        } catch (BadCredentialsException e) {
            log.warn(logMsg.get("auth.log.login.bad.credentials", identify));
            throw new InvalidCredentialsException(identify);
        }
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    /**
     * Logs out the current user by blacklisting the JWT token.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Retrieves current token from ThreadLocal</li>
     *   <li>Adds token to blacklist to prevent reuse</li>
     *   <li>Clears SecurityContext</li>
     *   <li>Generates new tokens for next session</li>
     * </ol>
     * </p>
     *
     * <p><b>Note:</b> The token is blacklisted but not physically deleted.
     * It will be rejected on subsequent requests until it expires.</p>
     *
     * @return new authentication response with fresh tokens for next session
     */
    @Transactional
    public AuthResponse logout() {
        String currentToken = JwtTokenHolder.getToken();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser) {
            User user = userSearchService.getUserById(securityUser.getUserId());

            if (currentToken != null && !currentToken.isEmpty()) {
                addTokenToBlacklist(currentToken, securityUser.getUserId());
                String tokenPreview = getTokenPreview(currentToken);
                log.info(logMsg.get("auth.log.token.blacklisted", tokenPreview));
            } else {
                log.warn("No token found in ThreadLocal for logout");
            }

            SecurityContextHolder.clearContext();

            AuthResponse newTokens = buildAuthResponse(securityUser, user);

            log.info(logMsg.get("auth.log.logout.success", user.getEmail()));

            return newTokens;
        }

        log.info(logMsg.get("auth.log.logout.success"));
        SecurityContextHolder.clearContext();

        return AuthResponse.builder()
                .tokenType(TOKEN_TYPE)
                .build();
    }

    // =========================================================================
    // TOKEN VALIDATION
    // =========================================================================

    /**
     * Validates the current authentication token.
     *
     * <p>Checks that the user is authenticated and returns user details.</p>
     *
     * @return user response with profile information
     */
    public UserResponse validateToken() {
        Authentication authentication = authenticationValidator.validateAuthentication();
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        assert securityUser != null;
        log.debug(logMsg.get("auth.log.token.valid", securityUser.getUsername()));

        User user = userSearchService.getUserById(securityUser.getUserId());

        return userMapper.build(user);
    }

    // =========================================================================
    // TOKEN REFRESH
    // =========================================================================

    /**
     * Refreshes JWT tokens using a valid refresh token.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Validates that refresh token is present</li>
     *   <li>Checks token not blacklisted</li>
     *   <li>Validates token signature and expiration</li>
     *   <li>Extracts user ID and loads SecurityUser</li>
     *   <li>Checks account is enabled</li>
     *   <li>Generates new token pair</li>
     * </ol>
     * </p>
     *
     * @param refreshToken the refresh token from the client
     * @return new authentication response with fresh tokens
     * @throws RefreshTokenMissingException if refresh token is not provided
     * @throws TokenExpiredException        if token is expired or invalid
     * @throws AccountDeactivatedException  if account is disabled
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug(logMsg.get("auth.log.refresh.attempt"));

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn(logMsg.get("auth.log.refresh.token.missing"));
            throw new RefreshTokenMissingException();
        }

        if (blacklistedTokenRepository.existsByToken(refreshToken)) {
            log.warn("Refresh token is blacklisted");
            throw new TokenExpiredException();
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn(logMsg.get("auth.log.refresh.token.invalid"));
            throw new TokenExpiredException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        log.debug(logMsg.get("auth.log.refresh.userid.extracted", userId));

        SecurityUser securityUser = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("auth.log.security.user.not.found", userId));
                    return new SecurityUserNotFoundException(userId);
                });

        if (!securityUser.isEnabled()) {
            log.warn(logMsg.get("auth.log.refresh.user.inactive", userId));
            throw new AccountDeactivatedException(userId);
        }

        User user = userSearchService.getUserById(userId);

        log.info(logMsg.get("auth.log.refresh.success", userId));

        return buildAuthResponse(securityUser, user);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Adds a JWT token to the blacklist.
     *
     * <p>Blacklisted tokens are rejected on subsequent requests.
     * Expired tokens are automatically cleaned up during this operation.</p>
     *
     * @param token  the JWT token to blacklist
     * @param userId ID of the user associated with the token
     */
    private void addTokenToBlacklist(String token, Long userId) {
        try {
            LocalDateTime expiresAt = jwtTokenProvider.getExpirationDateFromToken(token);

            if (blacklistedTokenRepository.existsByToken(token)) {
                log.debug("Token already in blacklist");
                return;
            }

            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .token(token)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .blacklistedAt(LocalDateTime.now())
                    .build();

            blacklistedTokenRepository.save(blacklistedToken);

            log.debug("Token added to blacklist, expires at: {}", expiresAt);

            int deleted = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            if (deleted > 0) {
                log.debug("Deleted {} expired tokens from blacklist", deleted);
            }

        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds an authentication response with generated tokens.
     *
     * @param securityUser the security user entity
     * @param user         the domain user entity
     * @return complete authentication response with tokens and user details
     */
    private AuthResponse buildAuthResponse(SecurityUser securityUser, User user) {
        String accessToken = jwtTokenProvider.createToken(securityUser, user);
        String refreshToken = jwtTokenProvider.createRefreshToken(securityUser, user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtTokenProvider.getValidity())
                .user(userMapper.build(user))
                .build();
    }

    /**
     * Creates a safe preview of a token for logging purposes.
     *
     * <p>Truncates the token to prevent full token exposure in logs.</p>
     *
     * @param token the full JWT token
     * @return truncated token preview (first N characters + "...")
     */
    private String getTokenPreview(String token) {
        if (token == null || token.isEmpty()) {
            return "empty";
        }
        int endIndex = Math.min(TOKEN_PREVIEW_MAX_LENGTH, token.length());
        return token.substring(0, endIndex) + TOKEN_ELLIPSIS;
    }
}