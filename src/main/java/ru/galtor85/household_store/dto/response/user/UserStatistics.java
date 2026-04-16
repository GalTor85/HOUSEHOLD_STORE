package ru.galtor85.household_store.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User statistics DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User statistics data")
public class UserStatistics {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    @Schema(description = "Total number of users", example = "150")
    private long totalUsers;

    @Schema(description = "Number of active users", example = "120")
    private long activeUsers;

    @Schema(description = "Number of inactive users", example = "30")
    private long inactiveUsers;

    @Schema(description = "Number of administrators", example = "5")
    private long admins;

    @Schema(description = "Number of managers", example = "10")
    private long managers;

    @Schema(description = "Number of regular users", example = "135")
    private long regularUsers;

    @Schema(description = "Percentage of active users", example = "80.0")
    private double activePercentage;

    @Schema(description = "Percentage of administrators", example = "3.33")
    private double adminPercentage;

    /**
     * Constructor with automatic percentage calculation.
     */
    public UserStatistics(long totalUsers, long activeUsers, long inactiveUsers,
                          long admins, long managers, long regularUsers) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
        this.admins = admins;
        this.managers = managers;
        this.regularUsers = regularUsers;
        this.activePercentage = calculatePercentage(activeUsers, totalUsers);
        this.adminPercentage = calculatePercentage(admins, totalUsers);
    }

    private double calculatePercentage(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((part * PERCENTAGE_MULTIPLIER / total) * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
    }
}