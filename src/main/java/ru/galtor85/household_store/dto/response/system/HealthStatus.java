package ru.galtor85.household_store.dto.response.system;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
}