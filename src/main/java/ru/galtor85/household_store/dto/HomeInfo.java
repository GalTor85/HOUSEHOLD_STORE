package ru.galtor85.household_store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeInfo {
    private String applicationName;
    private String version;
    private String status;
    private LocalDateTime currentTime;
    private String uptime;
    private String[] endpoints;
}