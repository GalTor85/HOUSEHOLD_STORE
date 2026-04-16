package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoice Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Invoice Data Transfer Object", title = "Invoice DTO")
public class InvoiceDto {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Schema(description = "Unique invoice identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Unique invoice number", example = "INV-20240330-001")
    private String invoiceNumber;

    // =========================================================================
    // ORDER REFERENCES
    // =========================================================================

    @Schema(description = "Purchase order ID (for supplier invoices)", example = "5")
    private Long purchaseOrderId;

    @Schema(description = "Purchase order number", example = "PO-20240330-001")
    private String purchaseOrderNumber;

    @Schema(description = "Sales order ID (for customer invoices)", example = "10")
    private Long salesOrderId;

    @Schema(description = "Sales order number", example = "SO-20240330-001")
    private String salesOrderNumber;

    @Schema(description = "Order type", example = "PURCHASE", allowableValues = {"PURCHASE", "SALES"})
    private String orderTypeDescription;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @Schema(description = "Invoice amount", example = "8500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "RUB", defaultValue = "RUB")
    private String currency;

    @Schema(description = "Total paid amount", example = "3500.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal totalPaid;

    @Schema(description = "Remaining amount to pay", example = "5000.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal remainingAmount;

    @Schema(description = "Payment percentage (0-100)", example = "41.18", accessMode = Schema.AccessMode.READ_ONLY)
    private Double paymentPercent;

    // =========================================================================
    // STATUS
    // =========================================================================

    @Schema(description = "Invoice status", example = "PARTIALLY_PAID")
    private InvoiceStatus status;

    @Schema(description = "Localized status name", example = "Partially Paid")
    private String localizedStatus;

    // =========================================================================
    // PAYMENT METHOD
    // =========================================================================

    @Schema(description = "Payment method", example = "BANK_TRANSFER")
    private PaymentMethod paymentMethod;

    @Schema(description = "Localized payment method name", example = "Bank Transfer")
    private String localizedPaymentMethod;

    // =========================================================================
    // DATES
    // =========================================================================

    @Schema(description = "Invoice issue date", example = "2024-03-30T18:04:30.424557")
    private LocalDateTime issueDate;

    @Schema(description = "Invoice due date", example = "2024-04-29T18:04:30.424557")
    private LocalDateTime dueDate;

    @Schema(description = "Payment date (when invoice was fully paid)", example = "2024-03-31T15:30:00")
    private LocalDateTime paidDate;

    // =========================================================================
    // ADDITIONAL INFORMATION
    // =========================================================================

    @Schema(description = "Invoice description", example = "Payment for purchase order PO-20240330-001")
    private String description;

    @Schema(description = "Additional notes", example = "Please pay by due date")
    private String notes;

    @Schema(description = "ID of user who created the invoice", example = "1")
    private Long createdBy;

    @Schema(description = "Creation timestamp", example = "2024-03-30T18:04:30.428554")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-03-31T15:30:00")
    private LocalDateTime updatedAt;
}