package ru.galtor85.household_store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User statistics data", title = "User Statistics")
public class UserStatistics {

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

    // Конструктор с автоматическим расчетом процентов
    public UserStatistics(long totalUsers, long activeUsers, long inactiveUsers,
                          long admins, long managers, long regularUsers) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
        this.admins = admins;
        this.managers = managers;
        this.regularUsers = regularUsers;

        // Рассчитываем проценты
        this.activePercentage = totalUsers > 0 ?
                Math.round((activeUsers * 100.0 / totalUsers) * 100) / 100.0 : 0;
        this.adminPercentage = totalUsers > 0 ?
                Math.round((admins * 100.0 / totalUsers) * 100) / 100.0 : 0;
    }

    // Статический фабричный метод
    public static UserStatistics of(long totalUsers, long activeUsers, long inactiveUsers,
                                    long admins, long managers, long regularUsers) {
        return new UserStatistics(totalUsers, activeUsers, inactiveUsers,
                admins, managers, regularUsers);
    }

    // Методы для проверки
    public boolean hasAdmins() {
        return admins > 0;
    }

    public boolean hasManagers() {
        return managers > 0;
    }

    public boolean hasRegularUsers() {
        return regularUsers > 0;
    }

    public boolean isAllUsersActive() {
        return totalUsers > 0 && activeUsers == totalUsers;
    }

    public boolean hasNoUsers() {
        return totalUsers == 0;
    }
}