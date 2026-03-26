package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Currency create request DTO")
public class CurrencyCreateRequest {

    @NotBlank(message = "{currency.validation.code.empty}")
    @Pattern(regexp = "^[A-Z]{3}$", message = "{currency.validation.code.pattern}")
    @Schema(description = "Currency code (ISO 4217)", example = "RUB", required = true)
    private String code;

    @NotBlank(message = "{currency.validation.name.empty}")
    @Size(min = 2, max = 100, message = "{currency.validation.name.size}")
    @Schema(description = "Currency name", example = "Russian Ruble", required = true)
    private String name;

    @NotBlank(message = "{currency.validation.symbol.empty}")
    @Size(min = 1, max = 5, message = "{currency.validation.symbol.size}")
    @Schema(description = "Currency symbol", example = "₽", required = true)
    private String symbol;

    @Schema(description = "Is base currency", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean isBase = false;

    @NotNull(message = "{currency.validation.exchange.rate.empty}")
    @DecimalMin(value = "0.0001", message = "{currency.validation.exchange.rate.min}")
    @Schema(description = "Exchange rate to base currency", example = "1.0000", required = true)
    private BigDecimal exchangeRate;

    @Schema(description = "Number of decimal places", example = "2", defaultValue = "2")
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Schema(description = "Is active", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Проверяет, является ли валюта базовой
     */
    public boolean isBaseCurrency() {
        return Boolean.TRUE.equals(isBase);
    }

    /**
     * Проверяет, активна ли валюта
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Получает код валюты в верхнем регистре
     */
    public String getNormalizedCode() {
        return code != null ? code.toUpperCase().trim() : null;
    }

    /**
     * Получает название валюты (обрезает пробелы)
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    /**
     * Получает символ валюты (обрезает пробелы)
     */
    public String getNormalizedSymbol() {
        return symbol != null ? symbol.trim() : null;
    }
}