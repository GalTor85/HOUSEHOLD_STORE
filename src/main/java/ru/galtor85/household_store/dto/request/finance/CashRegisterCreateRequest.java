package ru.galtor85.household_store.dto.request.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Schema(description = "Request DTO for creating a new cash register", title = "Cash Register Create Request")
public class CashRegisterCreateRequest {

    @NotBlank(message = "Cash register number cannot be empty")
    @Pattern(regexp = "^[A-Z0-9-]{3,20}$", message = "Cash register number must contain only uppercase letters, numbers and hyphens, length 3-20")
    @Schema(description = "Unique cash register number",
            example = "REG-001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String registerNumber;

    @NotBlank(message = "Cash register name cannot be empty")
    @Size(min = 2, max = 100, message = "Cash register name must be between 2 and 100 characters")
    @Schema(description = "Display name of the cash register",
            example = "Main Cash Register",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 200, message = "Location cannot exceed 200 characters")
    @Schema(description = "Physical location or description",
            example = "Main hall, counter #1")
    private String location;

    @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
    @Schema(description = "Initial cash balance when opening the register",
            example = "10000.00",
            defaultValue = "0.00")
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
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