package ru.galtor85.household_store.dto.request.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.FinancialConstants.MIN_EXCHANGE_RATE_STR;

/**
 * Request DTO for updating an existing currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Currency update request DTO")
public class CurrencyUpdateRequest {

    @Schema(description = "Currency name", example = "US Dollar")
    private String name;

    @Schema(description = "Currency symbol", example = "$")
    private String symbol;

    @DecimalMin(value = MIN_EXCHANGE_RATE_STR, message = "{currency.validation.exchange.rate.min}")
    @Schema(description = "Exchange rate to base currency", example = "0.0125")
    private BigDecimal exchangeRate;

    @Schema(description = "Number of decimal places", example = "2")
    private Integer decimalPlaces;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isNameUpdating() {
        return name != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSymbolUpdating() {
        return symbol != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isExchangeRateUpdating() {
        return exchangeRate != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDecimalPlacesUpdating() {
        return decimalPlaces != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isActiveUpdating() {
        return isActive != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSymbol() {
        return symbol != null ? symbol.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean getActiveValue() {
        return Boolean.TRUE.equals(isActive);
    }
}