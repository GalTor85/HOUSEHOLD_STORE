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
@Schema(description = "Home page information DTO", title = "Home Info")
public class HomeInfo {

    @Schema(description = "Application name", example = "Household Store")
    private String applicationName;

    @Schema(description = "Application version", example = "1.0.0")
    private String version;

    @Schema(description = "Application status", example = "RUNNING")
    private String status;

    @Schema(description = "Current server time", example = "2024-01-01T12:00:00")
    private LocalDateTime currentTime;

    @Schema(description = "System uptime", example = "1d 2h 30m 15s")
    private String uptime;

    @Schema(description = "Available API endpoints")
    private String[] endpoints;
}