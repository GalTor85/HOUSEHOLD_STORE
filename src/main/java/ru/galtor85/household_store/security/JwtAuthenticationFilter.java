package ru.galtor85.household_store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.galtor85.household_store.advice.exception.auth.TokenExpiredException;
import ru.galtor85.household_store.advice.exception.auth.TokenMalformedException;
import ru.galtor85.household_store.advice.exception.auth.TokenSecurityException;
import ru.galtor85.household_store.advice.exception.auth.TokenUnsupportedException;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.repository.security.BlacklistedTokenRepository;
import ru.galtor85.household_store.service.auth.JwtTokenHolder;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.io.IOException;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * JWT Authentication Filter for processing JSON Web Tokens in incoming requests.
 *
 * <p>This filter intercepts HTTP requests, extracts JWT from Authorization header,
 * validates it, and sets up Spring Security context with authenticated user.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Extracts JWT from Bearer token in Authorization header</li>
 *   <li>Validates token signature, expiration, and format</li>
 *   <li>Checks if token is blacklisted (logged out)</li>
 *   <li>Loads user details and establishes security context</li>
 *   <li>Stores token in {@link JwtTokenHolder} for request-scoped access</li>
 *   <li>Returns proper error responses for various token validation failures</li>
 * </ul>
 *
 * <h3>Error Responses:</h3>
 * <ul>
 *   <li>{@code 401 Unauthorized} - Token expired, malformed, unsupported, or blacklisted</li>
 *   <li>{@code 401 Unauthorized} - Invalid token signature or security error</li>
 * </ul>
 *
 * <h3>Token Blacklist:</h3>
 * <p>When a user logs out, the token is added to the blacklist.
 * This filter rejects any blacklisted token, preventing its reuse.</p>
 *
 * <h3>ThreadLocal Token Storage:</h3>
 * <p>The token is stored in {@link JwtTokenHolder} (ThreadLocal) to allow
 * access in services without passing it through method parameters.</p>
 *
 * @author G@LTor85
 * @see JwtTokenProvider
 * @see JwtTokenHolder
 * @see OncePerRequestFilter
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /**
     * URI path for logout endpoint.
     * Used to determine whether to clear the token from ThreadLocal after request.
     */
    private static final String LOGOUT_URI_PATH = "/logout";

    /**
     * Authorization header name.
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Length of "Bearer " prefix in Authorization header (7 characters).
     */
    private static final int TOKEN_PREFIX_LENGTH = 7;

    // =========================================================================
    // FIELDS
    // =========================================================================

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final MessageService messageService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final ObjectMapper objectMapper = createObjectMapper();

    // =========================================================================
    // OBJECT MAPPER CONFIGURATION
    // =========================================================================

    /**
     * Creates and configures ObjectMapper for JSON serialization.
     *
     * <p>Registers JavaTimeModule for Java 8+ date/time support and
     * disables writing dates as timestamps for better readability.</p>
     *
     * @return configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // =========================================================================
    // FILTER METHODS
    // =========================================================================

    /**
     * Processes each incoming request, performing JWT authentication.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Extract JWT from Authorization header</li>
     *   <li>Store token in ThreadLocal for request-scoped access</li>
     *   <li>Check if token is blacklisted</li>
     *   <li>Validate token (expiration, signature, format)</li>
     *   <li>Load user details and set authentication in SecurityContext</li>
     *   <li>Clear ThreadLocal after request completion</li>
     * </ol>
     * </p>
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain filter chain for continuing request processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                JwtTokenHolder.setToken(jwt);

                try {
                    if (blacklistedTokenRepository.existsByToken(jwt)) {
                        log.warn("Token is blacklisted (logout)");
                        sendApiErrorResponse(response, "auth.error.token.blacklisted", HttpStatus.UNAUTHORIZED);
                        return;
                    }

                    if (jwtTokenProvider.validateToken(jwt)) {
                        String email = jwtTokenProvider.getUsernameFromToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Authentication set for user: {}", email);
                    }
                } finally {
                    // Token cleanup is handled in outer finally block
                }
            }

        } catch (TokenExpiredException e) {
            log.warn("Token expired");
            sendApiErrorResponse(response, "auth.error.token.expired", HttpStatus.UNAUTHORIZED);
            return;
        } catch (TokenMalformedException e) {
            log.warn("Token malformed");
            sendApiErrorResponse(response, "auth.error.token.malformed", HttpStatus.UNAUTHORIZED);
            return;
        } catch (TokenUnsupportedException e) {
            log.warn("Token unsupported");
            sendApiErrorResponse(response, "auth.error.token.unsupported", HttpStatus.UNAUTHORIZED);
            return;
        } catch (TokenSecurityException e) {
            log.warn("Token security error");
            sendApiErrorResponse(response, "auth.error.token.security", HttpStatus.UNAUTHORIZED);
            return;
        } catch (Exception ex) {
            log.error("JWT authentication error: {}", ex.getMessage(), ex);
            sendApiErrorResponse(response, "auth.error.token.invalid", HttpStatus.UNAUTHORIZED);
            return;
        } finally {
            if (!isLogoutRequest(request)) {
                JwtTokenHolder.clear();
            }
        }

        filterChain.doFilter(request, response);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Determines if the current request is a logout request.
     *
     * <p>For logout requests, the token is NOT cleared from ThreadLocal
     * because it needs to be available for the logout handler.</p>
     *
     * @param request HTTP request
     * @return {@code true} if request is for logout endpoint
     */
    private boolean isLogoutRequest(HttpServletRequest request) {
        return request.getRequestURI().contains(LOGOUT_URI_PATH);
    }

    /**
     * Extracts JWT token from Authorization header.
     *
     * <p>Expects header format: {@code "Bearer <token>"}</p>
     *
     * @param request HTTP request
     * @return JWT token string, or {@code null} if not present or invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX_LENGTH);
        }
        return null;
    }

    /**
     * Sends an error response with localized message.
     *
     * <p>Creates a JSON response with {@link ApiResponse} format,
     * sets appropriate HTTP status code and Content-Type headers.</p>
     *
     * @param response   HTTP response
     * @param messageKey message key for localization
     * @param status     HTTP status to return
     * @throws IOException if writing response fails
     */
    private void sendApiErrorResponse(HttpServletResponse response, String messageKey, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(UTF_8_ENCODING);

        String message = messageService.get(messageKey);
        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}