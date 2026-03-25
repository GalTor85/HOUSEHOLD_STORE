package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Currency DTO")
public class CurrencyDto {

    @Schema(description = "Currency ID", example = "1")
    private Long id;  // ← ДОБАВИТЬ ЭТО ПОЛЕ

    @Schema(description = "Currency code", example = "RUB")
    private String code;

    @Schema(description = "Currency name", example = "Российский рубль")
    private String name;

    @Schema(description = "Localized currency name", example = "Российский рубль")
    private String localizedName;

    @Schema(description = "Currency symbol", example = "₽")
    private String symbol;

    @Schema(description = "Localized currency symbol", example = "руб.")
    private String localizedSymbol;

    @Schema(description = "Is base currency", example = "true")
    private Boolean isBase;

    @Schema(description = "Exchange rate to base currency", example = "1.0000")
    private BigDecimal exchangeRate;

    @Schema(description = "Number of decimal places", example = "2")
    private Integer decimalPlaces;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Created by user ID", example = "1")
    private Long createdBy;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at timestamp")
    private LocalDateTime updatedAt;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public String format(BigDecimal amount) {
        if (amount == null) {
            return "0.00 " + symbol;
        }
        return String.format("%,." + decimalPlaces + "f %s", amount, symbol);
    }

    public String formatWithCode(BigDecimal amount) {
        if (amount == null) {
            return "0.00 " + code;
        }
        return String.format("%,." + decimalPlaces + "f %s", amount, code);
    }

    public boolean isBaseCurrency() {
        return Boolean.TRUE.equals(isBase);
    }

    public boolean isActiveCurrency() {
        return Boolean.TRUE.equals(isActive);
    }
}