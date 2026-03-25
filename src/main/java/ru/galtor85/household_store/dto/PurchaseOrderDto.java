package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Purchase salesOrder DTO")
public class PurchaseOrderDto {

    @Schema(description = "Purchase salesOrder ID", example = "1")
    private Long id;

    @Schema(description = "SalesOrder number", example = "PO-20240322-001")
    private String orderNumber;

    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @Schema(description = "Supplier name", example = "ООО ТехноПост")
    private String supplierName;

    @Schema(description = "SalesOrder status")
    private OrderStatus status;

    @Schema(description = "Localized status", example = "Ожидает оплаты")
    private String localizedStatus;

    @Schema(description = "SalesOrder items")
    private List<PurchaseOrderItemDto> items;

    // Поля для закупки
    @Schema(description = "Expected delivery date", example = "2024-03-25")
    private LocalDate expectedDelivery;

    @Schema(description = "Actual delivery date", example = "2024-03-22")
    private LocalDate actualDelivery;

    @Schema(description = "Warehouse location", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Schema(description = "Who received the goods", example = "1")
    private Long receivedBy;

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Invoice number", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Payment due date", example = "2024-04-10")
    private LocalDate paymentDue;

    @Schema(description = "Payment status", example = "PENDING")
    private String paymentStatus;

    // Финансовые поля
    @Schema(description = "Subtotal", example = "85000.00")
    private BigDecimal subtotal;

    @Schema(description = "Total amount", example = "85000.00")
    private BigDecimal totalAmount;

    // Административные поля
    @Schema(description = "Created by manager ID", example = "1")
    private Long createdBy;

    @Schema(description = "Notes", example = "Urgent delivery")
    private String notes;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    @Schema(description = "Total received items count", example = "50")
    private Integer totalReceivedItems;

    @Schema(description = "Total received amount", example = "42500.00")
    private BigDecimal totalReceivedAmount;
}