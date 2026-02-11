package ru.galtor85.household_store.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final Environment environment;
    private final DataSource dataSource;
    private final BuildProperties buildProperties;

    // Время запуска приложения
    private final Instant startupTime = Instant.now();

    public String getUptime() {
        Duration uptime = Duration.between(startupTime, Instant.now());
        return String.format("%dд %dч %dм %dс",
                uptime.toDays(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());
    }

    public String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return "CONNECTED";
            }
        } catch (Exception e) {
            log.error("Database connection check failed", e);
        }
        return "DISCONNECTED";
    }

    public String checkDiskSpace() {
        // Простая проверка дискового пространства
        // В реальном приложении можно использовать FileStore
        return "OK";
    }

    public String getSpringVersion() {
        return buildProperties.getVersion();
    }

    public String getEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ?
                String.join(", ", activeProfiles) : "default";
    }

    public String getServerInfo() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}