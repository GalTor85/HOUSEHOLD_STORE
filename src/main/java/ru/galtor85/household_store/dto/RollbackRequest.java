package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rollback request DTO", title = "Rollback Request")
public class RollbackRequest {

    @NotNull(message = "{rollback.validation.order.id.empty}")
    @Schema(description = "Order ID to rollback", example = "123", required = true)
    private Long orderId;

    @NotBlank(message = "{rollback.validation.reason.empty}")
    @Schema(description = "Reason for rollback", example = "Customer changed mind", required = true)
    private String reason;

    @Schema(description = "Additional comments", example = "Customer agreed to pay restocking fee")
    private String comments;
}