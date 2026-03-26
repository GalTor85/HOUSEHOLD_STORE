package ru.galtor85.household_store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.galtor85.household_store.service.JwtTokenHolder;

import java.io.IOException;

@Slf4j
@Component
public class JwtTokenCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            // ✅ Всегда очищаем токен после завершения запроса
            JwtTokenHolder.clear();
            log.debug("Token cleared from ThreadLocal");
        }
    }
}