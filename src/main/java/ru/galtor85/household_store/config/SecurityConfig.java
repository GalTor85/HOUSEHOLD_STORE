package ru.galtor85.household_store.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.galtor85.household_store.advice.exception.auth.AuthenticationManagerException;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.security.JwtAuthenticationFilter;
import ru.galtor85.household_store.security.JwtTokenCleanupFilter;
import ru.galtor85.household_store.service.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.Arrays;

import static ru.galtor85.household_store.config.ApiConstants.API_BASE;

/**
 * Security configuration for the application.
 *
 * <p>This configuration sets up:</p>
 * <ul>
 *   <li>JWT-based authentication</li>
 *   <li>Stateless session management</li>
 *   <li>CORS configuration for frontend integration</li>
 *   <li>Role-based authorization for endpoints</li>
 *   <li>Password encoding with BCrypt</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a filter that cleans up JWT tokens from ThreadLocal after request completion.
     *
     * @return JwtTokenCleanupFilter instance
     */
    @Bean
    public JwtTokenCleanupFilter jwtTokenCleanupFilter() {
        return new JwtTokenCleanupFilter(messageService);
    }

    /**
     * Creates a password encoder using BCrypt hashing algorithm.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates the authentication manager for handling authentication requests.
     *
     * @param authConfig the authentication configuration
     * @return AuthenticationManager instance
     * @throws AuthenticationManagerException if authentication manager creation fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) {
        try {
            log.debug(messageService.get("security-config.log.creating.authentication.manager"));
            return authConfig.getAuthenticationManager();
        } catch (Exception e) {
            throw new AuthenticationManagerException(
                    messageService.get("security-config.error.creating.authentication.manager", e.getMessage())
            );
        }
    }

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * <p>This method defines:</p>
     * <ul>
     *   <li>CSRF protection (disabled for stateless REST API)</li>
     *   <li>CORS configuration</li>
     *   <li>Stateless session policy</li>
     *   <li>Public endpoints that don't require authentication</li>
     *   <li>Role-protected endpoints for admin and manager</li>
     *   <li>JWT filter integration</li>
     *   <li>Authentication and access denied handlers</li>
     * </ul>
     *
     * @param http the HttpSecurity to configure
     * @return SecurityFilterChain instance
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management for REST API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Request authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                API_BASE + "/auth/register",
                                API_BASE + "/auth/login",
                                API_BASE + "/auth/refresh",
                                API_BASE + "/",
                                API_BASE + "/media/**",
                                API_BASE + "/health",
                                API_BASE + "/info",
                                API_BASE + "/ping",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/**",
                                "/error",
                                API_BASE + "/debug/**"
                        ).permitAll()

                        // Admin-only endpoints
                        .requestMatchers(API_BASE + "/admin/**").hasRole("ADMIN")

                        // Admin and Manager endpoint
                        .requestMatchers(API_BASE + "/manager/**").hasAnyRole("ADMIN", "MANAGER")

                        // Authenticated user endpoints
                        .requestMatchers(
                                API_BASE + "/**"
                        ).authenticated()

                        // All other requests
                        .anyRequest().permitAll()
                )

                // Add JWT filter before standard authentication filte
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtTokenCleanupFilter(), JwtAuthenticationFilter.class)

                // Exception handling configuration
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn(messageService.get("security-config.log.error.authentication.failed",
                                    request.getRequestURI(), authException.getMessage()));

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.setCharacterEncoding("UTF-8");

                            ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                                    .success(false)
                                    .message(messageService.get("security-config.error.authentication.required"))
                                    .path(request.getRequestURI())
                                    .build();

                            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn(messageService.get("security-config.log.error.access.denied",
                                    request.getRequestURI(), accessDeniedException.getMessage()));

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.setCharacterEncoding("UTF-8");

                            ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                                    .success(false)
                                    .message(messageService.get("security-config.error.access.denied.message"))
                                    .path(request.getRequestURI())
                                    .build();

                            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        })
                );

        log.debug(messageService.get("security-config.log.security.filter.chain.configured"));
        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     *
     * <p>This allows the frontend application running on different origins
     * to make requests to this API.</p>
     *
     * <p>Configuration includes:</p>
     * <ul>
     *   <li>Allowed origins (localhost:3000, localhost:8080)</li>
     *   <li>Allowed HTTP methods (GET, POST, PUT, PATCH, DELETE, OPTIONS)</li>
     *   <li>Allowed headers (Authorization, Content-Type, etc.)</li>
     *   <li>Credentials support for cookies and authorization headers</li>
     *   <li>Preflight request cache duration (1 hour)</li>
     *   <li>Exposed headers for client access</li>
     * </ul>
     *
     * @return CorsConfigurationSource with CORS settings applied to all endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080"
        ));

        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Preflight request cache duration (seconds)
        configuration.setMaxAge(3600L);

        // Headers exposed to the client
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        log.debug(messageService.get("security-config.log.cors.allowed.headers", configuration.getAllowedHeaders()));
        log.debug(messageService.get("security-config.log.cors.allowed.methods", configuration.getAllowedMethods()));
        log.debug(messageService.get("security-config.log.cors.allowed.origins", configuration.getAllowedOrigins()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug(messageService.get("security-config.log.cors.configuration.source.configured"));

        return source;
    }
}