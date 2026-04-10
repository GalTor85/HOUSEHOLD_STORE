package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_COMMENTS_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_REASON_LENGTH;

/**
 * Request DTO to reverse purchase order receipt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reverse purchase order receipt")
public class ReverseReceiptRequest {

    @NotNull(message = "{reverse.receipt.validation.order.id.required}")
    @Schema(description = "Purchase order ID", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotBlank(message = "{reverse.receipt.validation.reason.required}")
    @Size(max = MAX_REASON_LENGTH, message = "{reverse.receipt.validation.reason.max}")
    @Schema(description = "Reason for reversal", example = "Damaged goods detected", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "Items to reverse (if null, reverses all items)")
    private List<ReverseReceiptItem> items;

    @Size(max = MAX_COMMENTS_LENGTH, message = "{reverse.receipt.validation.comments.max}")
    @Schema(description = "Additional comments", example = "Return to supplier")
    private String comments;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasComments() {
        return comments != null && !comments.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedReason() {
        return reason != null ? reason.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedComments() {
        return comments != null ? comments.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isReverseAllItems() {
        return items == null || items.isEmpty();
    }
}