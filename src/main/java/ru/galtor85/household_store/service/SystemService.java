package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.dto.ApiResponse;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final Environment environment;
    private final DataSource dataSource;
    private final BuildProperties buildProperties;
    private final MessageService messageService;

    private final Instant startupTime = Instant.now();

    public String getUptime(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Duration uptime = Duration.between(startupTime, Instant.now());

        String uptimeFormat = messageService.get("system-service.system.uptime.format");

        String uptimeString = String.format(uptimeFormat,
                uptime.toDays(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());

        log.debug(messageService.get("system-service.log.system.uptime", uptimeString));

        return uptimeString;
    }

    public String checkDatabase(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                log.debug(messageService.get("system-service.log.system.database.connected"));
                return messageService.get("system-service.system.database.connected");
            }
        } catch (Exception e) {
            log.error(messageService.get("system-service.log.system.database.error", e.getMessage()), e);
        }

        return messageService.get("system-service.system.database.disconnected");
    }

    public String checkDiskSpace(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("system-service.log.system.disk.ok"));
        return messageService.get("system-service.system.disk.ok");
    }

    public String getSpringVersion(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String version = buildProperties.getVersion();
        log.debug(messageService.get("system-service.log.system.spring.version", version));

        return version;
    }

    public String getEnvironment(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String[] activeProfiles = environment.getActiveProfiles();
        String env = activeProfiles.length > 0 ?
                String.join(", ", activeProfiles) : "default";

        log.debug(messageService.get("system-service.log.system.environment", env));

        return env;
    }

    public String getServerInfo(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        String serverInfo = ManagementFactory.getRuntimeMXBean().getName();
        log.debug(messageService.get("system-service.log.system.server.info", serverInfo));

        return serverInfo;
    }

    // Перегруженные методы для обратной совместимости (без Locale)
    public String getUptime() {
        return getUptime(Locale.getDefault());
    }

    public String checkDatabase() {
        return checkDatabase(Locale.getDefault());
    }

    public String checkDiskSpace() {
        return checkDiskSpace(Locale.getDefault());
    }

    public String getSpringVersion() {
        return getSpringVersion(Locale.getDefault());
    }

    public String getEnvironment() {
        return getEnvironment(Locale.getDefault());
    }

    public String getServerInfo() {
        return getServerInfo(Locale.getDefault());
    }
}