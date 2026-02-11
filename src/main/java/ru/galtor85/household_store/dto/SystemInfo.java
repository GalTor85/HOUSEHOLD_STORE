package ru.galtor85.household_store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {
    private String javaVersion;
    private String springVersion;
    private String environment;
    private String serverInfo;
}