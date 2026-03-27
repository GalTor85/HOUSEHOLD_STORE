package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.repository.security.BlacklistedTokenRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final MessageService messageService;

    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2 часа ночи
    public void cleanupExpiredTokens() {
        log.info(messageService.get("auth.log.token.cleanup.start"));
        int deleted = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info(messageService.get("auth.log.token.cleanup.complete", deleted));
    }
}