package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System information DTO")
public class SystemInfo {

    @Schema(description = "Java version", example = "17.0.2")
    private String javaVersion;

    @Schema(description = "Spring Boot version", example = "3.0.0")
    private String springVersion;

    @Schema(description = "Active environment/profiles", example = "dev")
    private String environment;

    @Schema(description = "Server information", example = "localhost:8080")
    private String serverInfo;

    @Schema(description = "System uptime", example = "1d 2h 30m 15s")
    private String uptime;
}