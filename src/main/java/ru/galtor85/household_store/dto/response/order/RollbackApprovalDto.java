package ru.galtor85.household_store.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rollback approval DTO", title = "Rollback Approval")
public class RollbackApprovalDto {

    @Schema(description = "Approval ID", example = "1")
    private Long id;

    @Schema(description = "SalesOrder ID", example = "123")
    private Long orderId;

    @Schema(description = "Current salesOrder status", example = "SHIPPED")
    private String currentStatus;

    @Schema(description = "Target status after rollback", example = "PROCESSING")
    private String targetStatus;

    @Schema(description = "Manager who requested", example = "manager@example.com")
    private String requestedBy;

    @Schema(description = "Manager ID", example = "5")
    private Long requestedById;

    @Schema(description = "Reason for rollback", example = "Customer changed mind")
    private String reason;

    @Schema(description = "Additional comments", example = "Customer agreed to pay restocking fee")
    private String comments;

    @Schema(description = "Request timestamp", example = "2024-03-18T10:30:00")
    private LocalDateTime requestedAt;

    @Schema(description = "Approval status", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "Admin who approved/rejected", example = "admin@example.com")
    private String reviewedBy;

    @Schema(description = "Review timestamp", example = "2024-03-18T11:00:00")
    private LocalDateTime reviewedAt;

    @Schema(description = "Admin comments", example = "Approved with conditions")
    private String adminComments;
}