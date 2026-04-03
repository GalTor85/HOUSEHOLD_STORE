package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reverse purchase order receipt")
public class ReverseReceiptRequest {

    @NotNull(message = "Purchase order ID is required")
    @Schema(description = "Purchase order ID", example = "13", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotBlank(message = "Reason for reversal is required")
    @Schema(description = "Reason for reversal", example = "Damaged goods detected", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "Items to reverse (if null, reverses all items)")
    private List<ReverseReceiptItem> items;

    @Schema(description = "Additional comments", example = "Return to supplier")
    private String comments;
}