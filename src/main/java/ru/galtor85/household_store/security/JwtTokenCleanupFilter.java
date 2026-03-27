package ru.galtor85.household_store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.galtor85.household_store.service.auth.JwtTokenHolder;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenCleanupFilter extends OncePerRequestFilter {

   private final MessageService messageService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            //Всегда очищаем токен после завершения запроса
            JwtTokenHolder.clear();
            log.debug(messageService.get("token.cleared.from.threadlocal"));
        }
    }
}