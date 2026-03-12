package ru.galtor85.household_store.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
import ru.galtor85.household_store.advice.exception.AuthenticationManagerException;
import ru.galtor85.household_store.dto.ApiResponse;
import ru.galtor85.household_store.security.JwtAuthenticationFilter;
import ru.galtor85.household_store.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MessageService messageService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // Настройка CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Настройка сессий (stateless для REST)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Настройка авторизации запросов
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/",
                                "/api/v1/health",
                                "/api/v1/info",
                                "/api/v1/ping",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/**",
                                "/error",
                                "/api/v1/debug/**"
                        ).permitAll()

                        // Админские эндпоинты
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Менеджерские эндпоинты
                        .requestMatchers("/api/v1/manager/**").hasAnyRole("ADMIN", "MANAGER")

                        // Аутентифицированные пользователи
                        .requestMatchers("/api/v1/**").authenticated()

                        // Все остальные запросы
                        .anyRequest().permitAll()
                )

                // Добавляем JWT фильтр перед стандартным фильтром аутентификации
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // Настройка исключений
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn(messageService.get("security-config.log.error.authentication.failed",
                                    request.getRequestURI(), authException.getMessage()));

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                                    .success(false)
                                    .message(messageService.get("security-config.error.authentication.required"))
                                    .path(request.getRequestURI())
                                    .build();

                            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn(messageService.get("security-config.log.error.access.denied",
                                    request.getRequestURI(), accessDeniedException.getMessage()));

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                                    .success(false)
                                    .message(messageService.get("security-config.error.access.denied.message"))
                                    .path(request.getRequestURI())
                                    .build();

                            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                        })
                );

        log.debug(messageService.get("security-config.log.security.filter.chain.configured"));
        return http.build();
    }

    /**
     * Настройка CORS для разрешения запросов с фронтенда
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Разрешаем определенные источники
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080"
        ));

        // Разрешаем методы
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Разрешаем заголовки
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Разрешаем credentials (куки, авторизация)
        configuration.setAllowCredentials(true);

        // Время кеширования preflight запросов
        configuration.setMaxAge(3600L);

        // Разрешаем заголовки в ответе
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