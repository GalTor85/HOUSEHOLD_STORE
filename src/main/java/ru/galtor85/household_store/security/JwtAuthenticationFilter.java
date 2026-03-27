package ru.galtor85.household_store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final MessageService messageService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // ✅ Сохраняем токен в ThreadLocal для доступа в сервисах
                JwtTokenHolder.setToken(jwt);

                try {
                    // Проверяем, не в черном ли списке токен
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
                    // ✅ Не очищаем токен сразу, он нужен для logout
                    // Очистка будет после завершения всего запроса
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
            // ✅ Очищаем токен после обработки запроса
            // НО: для logout токен нужно сохранить до завершения
            // Используем request attribute для определения, нужно ли очищать
            if (!isLogoutRequest(request)) {
                JwtTokenHolder.clear();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return request.getRequestURI().contains("/logout");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendApiErrorResponse(HttpServletResponse response, String messageKey, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = messageService.get(messageKey);
        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}