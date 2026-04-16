package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_COMMENTS_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_REASON_LENGTH;

/**
 * Request DTO for order status rollback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rollback request DTO", title = "Rollback Request")
public class RollbackRequest {

    @NotNull(message = "{rollback.validation.salesOrder.id.empty}")
    @Schema(description = "SalesOrder ID to rollback", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotBlank(message = "{rollback.validation.reason.empty}")
    @Size(max = MAX_REASON_LENGTH, message = "{rollback.validation.reason.max}")
    @Schema(description = "Reason for rollback", example = "Customer changed mind", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Size(max = MAX_COMMENTS_LENGTH, message = "{rollback.validation.comments.max}")
    @Schema(description = "Additional comments", example = "Customer agreed to pay restocking fee")
    private String comments;
}