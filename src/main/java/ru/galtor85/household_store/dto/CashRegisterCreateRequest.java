package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash register create request DTO")
public class CashRegisterCreateRequest {

    @NotBlank(message = "{cash.register.validation.number.empty}")
    @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "{cash.register.validation.number.pattern}")
    @Schema(description = "Cash register number", example = "REG-001", required = true)
    private String registerNumber;

    @NotBlank(message = "{cash.register.validation.name.empty}")
    @Size(min = 2, max = 100, message = "{cash.register.validation.name.size}")
    @Schema(description = "Cash register name", example = "Основная касса", required = true)
    private String name;

    @Size(max = 200, message = "{cash.register.validation.location.size}")
    @Schema(description = "Location", example = "Главный зал, касса №1")
    private String location;

    @DecimalMin(value = "0.00", message = "{cash.register.validation.opening.balance.min}")
    @Schema(description = "Opening balance", example = "10000.00", defaultValue = "0.00")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Получает нормализованный номер кассы (верхний регистр, без пробелов)
     */
    public String getNormalizedRegisterNumber() {
        return registerNumber != null ? registerNumber.toUpperCase().trim() : null;
    }

    /**
     * Получает нормализованное название кассы
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    /**
     * Получает нормализованное местоположение
     */
    public String getNormalizedLocation() {
        return location != null ? location.trim() : null;
    }

    /**
     * Проверяет, является ли начальный баланс положительным
     */
    public boolean hasPositiveOpeningBalance() {
        return openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Проверяет, является ли начальный баланс нулевым
     */
    public boolean hasZeroOpeningBalance() {
        return openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}