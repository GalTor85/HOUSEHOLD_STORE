package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Currency DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Currency DTO")
public class CurrencyDto {

    @Schema(description = "Currency ID", example = "1")
    private Long id;

    @Schema(description = "Currency code", example = "RUB")
    private String code;

    @Schema(description = "Currency name (from database)", example = "Russian Ruble")
    private String name;

    @Schema(description = "Localized currency name", example = "Российский рубль")
    private String localizedName;

    @Schema(description = "Currency symbol (from database)", example = "RUB")
    private String symbol;

    @Schema(description = "Localized currency symbol", example = "₽")
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

    /**
     * Formats amount with currency symbol.
     *
     * @param amount amount to format
     * @return formatted amount string
     */
    public String format(BigDecimal amount) {
        if (amount == null) {
            return "0.00 " + symbol;
        }
        String formatPattern = "%,." + decimalPlaces + "f %s";
        return String.format(formatPattern, amount, symbol);
    }
}