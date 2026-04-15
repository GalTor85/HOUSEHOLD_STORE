package ru.galtor85.household_store.dto.request.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.FinancialConstants.*;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_ACTIVE_STATUS;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Currency create request DTO")
public class CurrencyCreateRequest {

    @NotBlank(message = "{currency.validation.code.empty}")
    @Pattern(regexp = CURRENCY_CODE_PATTERN, message = "{currency.validation.code.pattern}")
    @Schema(description = "Currency code (ISO 4217)", example = "RUB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "{currency.validation.name.empty}")
    @Schema(description = "Currency name", example = "Russian Ruble", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{currency.validation.symbol.empty}")
    @Schema(description = "Currency symbol", example = "₽", requiredMode = Schema.RequiredMode.REQUIRED)
    private String symbol;

    @Schema(description = "Is base currency", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean isBase = DEFAULT_IS_BASE;

    @NotNull(message = "{currency.validation.exchange.rate.empty}")
    @DecimalMin(value = MIN_EXCHANGE_RATE_STR, message = "{currency.validation.exchange.rate.min}")
    @Schema(description = "Exchange rate to base currency", example = "1.0000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal exchangeRate;

    @Schema(description = "Number of decimal places", example = "2", defaultValue = "2")
    @Builder.Default
    private Integer decimalPlaces = DEFAULT_DECIMAL_PLACES;

    @Schema(description = "Is active", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = DEFAULT_ACTIVE_STATUS;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Checks if the currency is base currency
     */
    public boolean isBaseCurrency() {
        return Boolean.TRUE.equals(isBase);
    }

    /**
     * Checks if the currency is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Gets normalized currency code (uppercase, trimmed)
     */
    public String getNormalizedCode() {
        return code != null ? code.toUpperCase().trim() : null;
    }

    /**
     * Gets normalized currency name (trimmed)
     */
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    /**
     * Gets normalized currency symbol (trimmed)
     */
    public String getNormalizedSymbol() {
        return symbol != null ? symbol.trim() : null;
    }
}