package ru.galtor85.household_store.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.advice.exception.system.DatabaseConnectionException;
import ru.galtor85.household_store.advice.exception.system.SystemInfoException;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Service for retrieving system information and health status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private static final int CONNECTION_VALIDITY_TIMEOUT = 2;
    private static final String DEFAULT_ENVIRONMENT = "default";

    private final Environment environment;
    private final DataSource dataSource;
    private final BuildProperties buildProperties;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    private final Instant startupTime = Instant.now();

    /**
     * Gets system uptime.
     *
     * @return formatted uptime string
     */
    public String getUptime() {
        try {
            Duration uptime = Duration.between(startupTime, Instant.now());
            String uptimeFormat = messageService.get("system-service.system.uptime.format");
            String uptimeString = String.format(uptimeFormat,
                    uptime.toDays(),
                    uptime.toHoursPart(),
                    uptime.toMinutesPart(),
                    uptime.toSecondsPart());

            log.debug(logMsg.get("system-service.log.system.uptime", uptimeString));
            return uptimeString;
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.uptime.error", e.getMessage()), e);
            throw new SystemInfoException("uptime",
                    messageService.get("system-service.error.uptime"), e);
        }
    }

    /**
     * Checks database connection status.
     *
     * @return database status message
     */
    public String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(CONNECTION_VALIDITY_TIMEOUT)) {
                log.debug(logMsg.get("system-service.log.system.database.connected"));
                return messageService.get("system-service.system.database.connected");
            } else {
                log.warn(logMsg.get("system-service.log.system.database.invalid"));
                throw new DatabaseConnectionException(
                        messageService.get("system-service.error.database.invalid"));
            }
        } catch (SQLException e) {
            log.error(logMsg.get("system-service.log.system.database.error", e.getMessage()), e);
            throw new DatabaseConnectionException(
                    messageService.get("system-service.error.database.connection"), e);
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.database.unexpected", e.getMessage()), e);
            throw new DatabaseConnectionException(
                    messageService.get("system-service.error.database.unexpected"), e);
        }
    }

    /**
     * Checks disk space status.
     *
     * @return disk space status message
     */
    public String checkDiskSpace() {
        try {
            log.debug(logMsg.get("system-service.log.system.disk.ok"));
            return messageService.get("system-service.system.disk.ok");
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.disk.error", e.getMessage()), e);
            throw new SystemInfoException("disk",
                    messageService.get("system-service.error.disk"), e);
        }
    }

    /**
     * Gets Spring Boot version.
     *
     * @return Spring version string
     */
    public String getSpringVersion() {
        try {
            String version = buildProperties.getVersion();
            log.debug(logMsg.get("system-service.log.system.spring.version", version));
            return version;
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.spring.version.error", e.getMessage()), e);
            throw new SystemInfoException("springVersion",
                    messageService.get("system-service.error.spring.version"), e);
        }
    }

    /**
     * Gets active environment profiles.
     *
     * @return active profiles string
     */
    public String getEnvironment() {
        try {
            String[] activeProfiles = environment.getActiveProfiles();
            String env = activeProfiles.length > 0
                    ? String.join(", ", activeProfiles)
                    : DEFAULT_ENVIRONMENT;

            log.debug(logMsg.get("system-service.log.system.environment", env));
            return env;
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.environment.error", e.getMessage()), e);
            throw new SystemInfoException("environment",
                    messageService.get("system-service.error.environment"), e);
        }
    }

    /**
     * Gets server information.
     *
     * @return server info string
     */
    public String getServerInfo() {
        try {
            String serverInfo = ManagementFactory.getRuntimeMXBean().getName();
            log.debug(logMsg.get("system-service.log.system.server.info", serverInfo));
            return serverInfo;
        } catch (Exception e) {
            log.error(logMsg.get("system-service.log.system.server.info.error", e.getMessage()), e);
            throw new SystemInfoException("serverInfo",
                    messageService.get("system-service.error.server.info"), e);
        }
    }
}