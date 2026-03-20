package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.advice.exception.DatabaseConnectionException;
import ru.galtor85.household_store.advice.exception.SystemInfoException;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final Environment environment;
    private final DataSource dataSource;
    private final BuildProperties buildProperties;
    private final MessageService messageService;

    private final Instant startupTime = Instant.now();

    public String getUptime() {
        try {
            Duration uptime = Duration.between(startupTime, Instant.now());

            String uptimeFormat = messageService.get("system-service.system.uptime.format");

            String uptimeString = String.format(uptimeFormat,
                    uptime.toDays(),
                    uptime.toHoursPart(),
                    uptime.toMinutesPart(),
                    uptime.toSecondsPart());

            log.debug(messageService.get("system-service.log.system.uptime", uptimeString));

            return uptimeString;
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.uptime.error", e.getMessage()), e);
            throw new SystemInfoException("uptime",
                    messageService.get("system-service.error.uptime"), e);
        }
    }

    public String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                log.debug(messageService.get("system-service.log.system.database.connected"));
                return messageService.get("system-service.system.database.connected");
            } else {
                log.warn(messageService.get("system-service.log.system.database.invalid"));
                throw new DatabaseConnectionException(
                        messageService.get("system-service.error.database.invalid"));
            }
        } catch (SQLException e) {
            log.error(messageService.get("system-service.log.system.database.error", e.getMessage()), e);
            throw new DatabaseConnectionException(
                    messageService.get("system-service.error.database.connection"), e);
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.database.unexpected", e.getMessage()), e);
            throw new DatabaseConnectionException(
                    messageService.get("system-service.error.database.unexpected"), e);
        }
    }

    public String checkDiskSpace() {
        try {
            // TODO: Реализовать реальную проверку дискового пространства
            log.debug(messageService.get("system-service.log.system.disk.ok"));
            return messageService.get("system-service.system.disk.ok");
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.disk.error", e.getMessage()), e);
            throw new SystemInfoException("disk",
                    messageService.get("system-service.error.disk"), e);
        }
    }

    public String getSpringVersion() {
        try {
            String version = buildProperties.getVersion();
            log.debug(messageService.get("system-service.log.system.spring.version", version));
            return version;
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.spring.version.error", e.getMessage()), e);
            throw new SystemInfoException("springVersion",
                    messageService.get("system-service.error.spring.version"), e);
        }
    }

    public String getEnvironment() {
        try {
            String[] activeProfiles = environment.getActiveProfiles();
            String env = activeProfiles.length > 0 ?
                    String.join(", ", activeProfiles) : "default";

            log.debug(messageService.get("system-service.log.system.environment", env));
            return env;
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.environment.error", e.getMessage()), e);
            throw new SystemInfoException("environment",
                    messageService.get("system-service.error.environment"), e);
        }
    }

    public String getServerInfo() {
        try {
            String serverInfo = ManagementFactory.getRuntimeMXBean().getName();
            log.debug(messageService.get("system-service.log.system.server.info", serverInfo));
            return serverInfo;
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.server.info.error", e.getMessage()), e);
            throw new SystemInfoException("serverInfo",
                    messageService.get("system-service.error.server.info"), e);
        }
    }
}