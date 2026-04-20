package ru.galtor85.household_store.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;
import java.util.Set;

/**
 * Filter that applies rate limiting to authentication endpoints.
 * <p>
 * Limits requests to login and registration endpoints to prevent
 * brute force attacks. Each client IP is limited to 10 requests per minute.
 * </p>
 *
 * @author G@LTor85
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    /** Endpoints protected by rate limiting */
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/app/auth/login",
            "/app/auth/register"
    );

    private final RateLimitingService rateLimitingService;

    /**
     * Filters incoming requests and applies rate limiting to protected paths.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @param chain    filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException      if I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        if (!rateLimitingEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (PROTECTED_PATHS.stream().anyMatch(path::equals)) {
            String clientIp = getClientIp(request);
            Bucket bucket = rateLimitingService.resolveBucket(clientIp + ":" + path);

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try later.\"}");
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Extracts client IP address from request.
     * <p>
     * Checks X-Forwarded-For header first (for proxy/load balancer),
     * falls back to remote address.
     * </p>
     *
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}