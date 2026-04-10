package ru.galtor85.household_store.dto.request.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new cash register", title = "Cash Register Create Request")
public class CashRegisterCreateRequest {

    @NotBlank(message = "{cash.register.validation.number.empty}")
    @Pattern(regexp = CASH_REGISTER_NUMBER_PATTERN, message = "{cash.register.validation.number.pattern}")
    @Schema(description = "Unique cash register number",
            example = "REG-001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String registerNumber;

    @NotBlank(message = "{cash.register.validation.name.empty}")
    @Schema(description = "Display name of the cash register",
            example = "Main Cash Register",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Physical location or description",
            example = "Main hall, counter #1")
    private String location;

    @DecimalMin(value = MIN_BALANCE_STR, message = "{cash.register.validation.opening.balance.min}")
    @Schema(description = "Initial cash balance when opening the register",
            example = "10000.00",
            defaultValue = DEFAULT_BALANCE_STR)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedRegisterNumber() {
        return registerNumber != null ? registerNumber.toUpperCase().trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedLocation() {
        return location != null ? location.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPositiveOpeningBalance() {
        return openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasZeroOpeningBalance() {
        return openingBalance != null && openingBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}