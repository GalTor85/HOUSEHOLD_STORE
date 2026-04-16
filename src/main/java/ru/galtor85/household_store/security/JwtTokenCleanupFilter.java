package ru.galtor85.household_store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.galtor85.household_store.service.auth.JwtTokenHolder;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.io.IOException;

/**
 * Filter for cleaning up JWT token from ThreadLocal after request completion.
 *
 * <p>This filter executes after the main JWT authentication filter and ensures
 * that the token is removed from ThreadLocal storage regardless of whether
 * the request was successful or resulted in an exception.</p>
 *
 * <p><b>Important:</b> The cleanup happens in a {@code finally} block to guarantee
 * execution even if an exception occurs during request processing. This prevents
 * memory leaks and cross-request token contamination.</p>
 *
 * <p><b>Execution order:</b> This filter runs after {@link JwtAuthenticationFilter}
 * to ensure the token is available during authentication but cleaned up before
 * the response is sent.</p>
 *
 * @author G@LTor85
 
 * @see JwtTokenHolder
 * @see JwtAuthenticationFilter
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class JwtTokenCleanupFilter extends OncePerRequestFilter {

    private final LogMessageService logMsg;

    /**
     * Processes the request and ensures ThreadLocal token cleanup.
     *
     * <p>The filter chain is executed first, and the token is cleared in the
     * {@code finally} block to ensure cleanup occurs in all scenarios.</p>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            JwtTokenHolder.clear();
            log.trace(logMsg.get("token.cleared.from.threadlocal"));
        }
    }
}