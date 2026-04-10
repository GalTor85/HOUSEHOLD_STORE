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
import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.*;

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
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenCleanupFilter jwtTokenCleanupFilter;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    private static final List<String> ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
    );

    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
            "Authorization",
            "Content-Disposition"
    );

    // =========================================================================
    // BEAN DEFINITIONS
    // =========================================================================

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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
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
     * @param http the HttpSecurity to configure
     * @return SecurityFilterChain instance
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ROOT).hasRole("ADMIN")
                        .requestMatchers(MANAGER_ROOT).hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(USER_ROOT).authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtTokenCleanupFilter, JwtAuthenticationFilter.class)
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
     * @return CorsConfigurationSource with CORS settings applied to all endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(corsMaxAge);
        configuration.setExposedHeaders(EXPOSED_HEADERS);

        log.debug(messageService.get("security-config.log.cors.allowed.headers", configuration.getAllowedHeaders()));
        log.debug(messageService.get("security-config.log.cors.allowed.methods", configuration.getAllowedMethods()));
        log.debug(messageService.get("security-config.log.cors.allowed.origins", configuration.getAllowedOrigins()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug(messageService.get("security-config.log.cors.configuration.source.configured"));

        return source;
    }
}