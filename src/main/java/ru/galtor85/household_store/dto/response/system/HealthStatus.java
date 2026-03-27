package ru.galtor85.household_store.dto.response.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health status DTO", title = "Health Status")
public class HealthStatus {

    @Schema(description = "Overall system status", example = "UP",
            allowableValues = {"UP", "DOWN", "DEGRADED"})
    private String status;

    @Schema(description = "Timestamp of the health check", example = "2024-01-01T12:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Database connection status", example = "CONNECTED",
            allowableValues = {"CONNECTED", "DISCONNECTED", "DEGRADED"})
    private String database;

    @Schema(description = "Disk space status", example = "OK",
            allowableValues = {"OK", "WARNING", "CRITICAL"})
    private String diskSpace;

    /**
     * Проверка, все ли компоненты работают нормально
     */
    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status)
                && "CONNECTED".equalsIgnoreCase(database)
                && "OK".equalsIgnoreCase(diskSpace);
    }

    /**
     * Получить отформатированное время
     */
    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return timestamp.format(formatter);
    }

    /**
     * Получить локализованный статус через MessageService
     */
    public String getLocalizedStatus(MessageService messageService) {
        if (status == null) return "";
        return messageService.get("health-status.status." + status.toLowerCase());
    }

    /**
     * Получить локализованный статус базы данных через MessageService
     */
    public String getLocalizedDatabase(MessageService messageService) {
        if (database == null) return "";
        return messageService.get("health-status.database." + database.toLowerCase());
    }

    /**
     * Получить локализованный статус диска через MessageService
     */
    public String getLocalizedDiskSpace(MessageService messageService) {
        if (diskSpace == null) return "";
        return messageService.get("health-status.diskspace." + diskSpace.toLowerCase());
    }
}