package ru.galtor85.household_store.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Purchase order DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Purchase order information")
public class PurchaseOrderDto {

    @Schema(description = "Purchase order ID", example = "1")
    private Long id;

    @Schema(description = "Order number", example = "PO-20240322-001")
    private String orderNumber;

    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @Schema(description = "Supplier name", example = "TechnoPost LLC")
    private String supplierName;

    @Schema(description = "Order status")
    private OrderStatus status;

    @Schema(description = "Localized status", example = "Awaiting payment")
    private String localizedStatus;

    @Schema(description = "Order items")
    private List<PurchaseOrderItemDto> items;

    @Schema(description = "Expected delivery date", example = "2024-03-25")
    private LocalDate expectedDelivery;

    @Schema(description = "Actual delivery date", example = "2024-03-22")
    private LocalDate actualDelivery;

    @Schema(description = "Warehouse location", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Schema(description = "Receiver user ID", example = "1")
    private Long receivedBy;

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Invoice number", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Payment due date", example = "2024-04-10")
    private LocalDate paymentDue;

    @Schema(description = "Payment status", example = "PENDING")
    private String paymentStatus;

    @Schema(description = "Localized payment status", example = "Awaiting payment")
    private String localizedPaymentStatus;

    @Schema(description = "Subtotal", example = "85000.00")
    private BigDecimal subtotal;

    @Schema(description = "Total amount", example = "85000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Created by manager ID", example = "1")
    private Long createdBy;

    @Schema(description = "Notes", example = "Urgent delivery")
    private String notes;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    @Schema(description = "Failed product IDs during receiving")
    private List<Long> failedItems;

    @Schema(description = "Error messages for failed placements")
    private List<String> errorMessages;
}