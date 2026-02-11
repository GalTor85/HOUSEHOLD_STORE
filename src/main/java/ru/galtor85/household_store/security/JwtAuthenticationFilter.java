package ru.galtor85.household_store.security;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("customUserDetailsService")
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Извлекаем JWT из запроса
            assert request != null;
            String jwt = getJwtFromRequest(request);

            // 2. Если токен существует и валиден
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // 3. Получаем email из токена
                String email = jwtTokenProvider.getUsernameFromToken(jwt);

                // 4. Загружаем данные пользователя
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Создаем объект аутентификации
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 6. Устанавливаем аутентификацию в контекст Security
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Установлен контекст аутентификации для пользователя: {}", email);
            }
        } catch (Exception ex) {
            log.error("Не удалось установить аутентификацию пользователя", ex);

            // Можно отправить более информативный ответ
            assert response != null;
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Ошибка аутентификации\", \"message\": \"" +
                            ex.getMessage() + "\"}"
            );
            return;
        }

        // 7. Продолжаем цепочку фильтров
        assert filterChain != null;
        filterChain.doFilter(request, response);
    }

    /**
     * Извлекает JWT токен из заголовка Authorization
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Игнорируем некоторые пути (опционально)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Не фильтруем публичные эндпоинты
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/health") ||
                path.equals("/api/v1/") ||
                path.equals("/api/v1/ping");
    }
}