package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(description = "Currency update request DTO")
public class CurrencyUpdateRequest {

    @Size(min = 2, max = 100, message = "{currency.validation.name.size}")
    @Schema(description = "Currency name", example = "US Dollar")
    private String name;

    @Size(min = 1, max = 5, message = "{currency.validation.symbol.size}")
    @Schema(description = "Currency symbol", example = "$")
    private String symbol;

    @DecimalMin(value = "0.0001", message = "{currency.validation.exchange.rate.min}")
    @Schema(description = "Exchange rate to base currency", example = "0.0125")
    private BigDecimal exchangeRate;

    @Schema(description = "Number of decimal places", example = "2")
    private Integer decimalPlaces;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Проверяет, есть ли хотя бы одно поле для обновления
     */
    public boolean hasUpdates() {
        return name != null || symbol != null || exchangeRate != null ||
                decimalPlaces != null || isActive != null;
    }

    /**
     * Проверяет, обновляется ли имя
     */
    public boolean isNameUpdating() {
        return name != null;
    }

    /**
     * Проверяет, обновляется ли символ
     */
    public boolean isSymbolUpdating() {
        return symbol != null;
    }

    /**
     * Проверяет, обновляется ли курс
     */
    public boolean isExchangeRateUpdating() {
        return exchangeRate != null;
    }

    /**
     * Проверяет, обновляется ли количество знаков после запятой
     */
    public boolean isDecimalPlacesUpdating() {
        return decimalPlaces != null;
    }

    /**
     * Проверяет, обновляется ли статус активности
     */
    public boolean isActiveUpdating() {
        return isActive != null;
    }

    /**
     * Получает нормализованное имя (обрезает пробелы)
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    /**
     * Получает нормализованный символ (обрезает пробелы)
     */
    public String getNormalizedSymbol() {
        return symbol != null ? symbol.trim() : null;
    }

    /**
     * Получает обновленное значение активности
     */
    public boolean getActiveValue() {
        return Boolean.TRUE.equals(isActive);
    }
}