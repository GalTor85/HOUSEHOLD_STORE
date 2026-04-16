package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.repository.security.BlacklistedTokenRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Scheduler for cleaning up expired JWT tokens.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final LogMessageService logMsg;

    @Scheduled(cron = "${app.scheduling.token-cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredTokens() {
        log.info(logMsg.get("auth.log.token.cleanup.start"));
        int deleted = blacklistedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info(logMsg.get("auth.log.token.cleanup.complete", deleted));
    }
}