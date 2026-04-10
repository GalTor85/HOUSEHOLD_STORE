package ru.galtor85.household_store.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_NOTES_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_QUANTITY;

/**
 * DTO for received item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Received item DTO", title = "Received Item")
public class ReceivedItemDto {

    @NotNull(message = "{received.item.validation.product.id.empty}")
    @Positive(message = "{received.item.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{received.item.validation.received.quantity.empty}")
    @Min(value = MIN_QUANTITY, message = "{received.item.validation.received.quantity.min}")
    @Schema(description = "Quantity actually received", example = "95", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer receivedQuantity;

    @Min(value = MIN_QUANTITY, message = "{received.item.validation.damaged.quantity.min}")
    @Schema(description = "Quantity damaged/rejected", example = "5")
    private Integer damagedQuantity;

    @Schema(description = "Quality check result", example = "true")
    private Boolean qualityPassed;

    @Size(max = MAX_NOTES_LENGTH, message = "{received.item.validation.notes.max}")
    @Schema(description = "Notes about this item", example = "5 units with damaged packaging")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasProductId() {
        return productId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasReceivedQuantity() {
        return receivedQuantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDamagedQuantity() {
        return damagedQuantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQualityPassed() {
        return qualityPassed != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isQualityPassedTrue() {
        return Boolean.TRUE.equals(qualityPassed);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalDamaged() {
        return damagedQuantity != null ? damagedQuantity : 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalReceived() {
        return receivedQuantity != null ? receivedQuantity : 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getAcceptedQuantity() {
        int received = getTotalReceived();
        int damaged = getTotalDamaged();
        return Math.max(received - damaged, 0);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDamages() {
        return getTotalDamaged() > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedNotes() {
        return notes != null ? notes.trim() : null;
    }
}