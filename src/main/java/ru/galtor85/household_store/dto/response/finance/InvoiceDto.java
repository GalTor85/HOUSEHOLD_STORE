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

    @Schema(description = "Unique invoice number", example = "INV-20240330-001", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "Localized order type description", example = "Purchase",
            allowableValues = {"Purchase", "Sales"})
    private String orderTypeDescription;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @Schema(description = "Invoice amount", example = "8500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "RUB", defaultValue = "RUB")
    private String currency;

    @Schema(description = "Total paid amount", example = "3500.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal totalPaid;

    @Schema(description = "Remaining amount to pay", example = "5000.00", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal remainingAmount;

    @Schema(description = "Number of payments made", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer paymentCount;

    @Schema(description = "Payment percentage (0-100)", example = "41.18", accessMode = Schema.AccessMode.READ_ONLY)
    private Double paymentPercent;

    // =========================================================================
    // STATUS
    // =========================================================================

    @Schema(description = "Invoice status", example = "PARTIALLY_PAID",
            allowableValues = {"PENDING", "PAID", "PARTIALLY_PAID", "CANCELLED", "REFUNDED"})
    private InvoiceStatus status;

    @Schema(description = "Localized status name", example = "Partially Paid")
    private String localizedStatus;

    // =========================================================================
    // PAYMENT METHOD
    // =========================================================================

    @Schema(description = "Payment method", example = "BANK_TRANSFER",
            allowableValues = {"CASH", "CARD", "BANK_TRANSFER", "ONLINE", "CREDIT"})
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

    // =========================================================================
    // LOCALIZED UI FIELDS
    // =========================================================================

    @Schema(description = "Localized remaining amount", example = "Remaining: 5,000.00 RUB")
    private String localizedRemainingAmount;

    @Schema(description = "Localized total paid", example = "Paid: 3,500.00 RUB")
    private String localizedTotalPaid;

    @Schema(description = "Localized payment percentage", example = "Paid 41.18%")
    private String localizedPaymentPercent;

    @Schema(description = "Localized due date", example = "Due: 29.04.2024 18:04")
    private String localizedDueDate;

    @Schema(description = "Localized issue date", example = "Issued: 30.03.2024 18:04")
    private String localizedIssueDate;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Checks if the invoice is overdue
     *
     * @return true if due date is passed and invoice is not paid
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) &&
                status == InvoiceStatus.PENDING;
    }

    /**
     * Checks if the invoice is fully paid
     *
     * @return true if remaining amount is zero or negative
     */
    public boolean isFullyPaid() {
        return remainingAmount != null && remainingAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Checks if the invoice is partially paid
     *
     * @return true if status is PARTIALLY_PAID
     */
    public boolean isPartiallyPaid() {
        return status == InvoiceStatus.PARTIALLY_PAID;
    }

    /**
     * Checks if the invoice can be paid
     *
     * @return true if status is PENDING or PARTIALLY_PAID
     */
    public boolean isPayable() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.PARTIALLY_PAID;
    }

    /**
     * Gets formatted amount with currency symbol
     *
     * @param currencySymbol currency symbol (₽, $, €)
     * @return formatted amount string
     */
    public String getFormattedAmount(String currencySymbol) {
        if (amount == null) return "0.00 " + currencySymbol;
        return String.format("%,.2f %s", amount, currencySymbol);
    }

    /**
     * Gets formatted remaining amount with currency symbol
     *
     * @param currencySymbol currency symbol (₽, $, €)
     * @return formatted remaining amount string
     */
    public String getFormattedRemainingAmount(String currencySymbol) {
        if (remainingAmount == null) return "0.00 " + currencySymbol;
        return String.format("%,.2f %s", remainingAmount, currencySymbol);
    }

    /**
     * Initializes localized fields for UI display
     *
     * @param messageService message service for localization
     * @param currencySymbol currency symbol
     */
    public void initLocalizedFields(ru.galtor85.household_store.service.i18n.MessageService messageService,
                                    String currencySymbol) {
        if (remainingAmount != null) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                localizedRemainingAmount = messageService.get("invoice.fully.paid");
            } else {
                localizedRemainingAmount = messageService.get("invoice.remaining.amount",
                        formatBalance(remainingAmount), currencySymbol);
            }
        }

        if (totalPaid != null) {
            localizedTotalPaid = messageService.get("invoice.total.paid",
                    formatBalance(totalPaid), currencySymbol);
        }

        if (paymentPercent != null) {
            localizedPaymentPercent = messageService.get("invoice.payment.percent",
                    String.format("%.2f", paymentPercent));
        }

        if (dueDate != null) {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            localizedDueDate = messageService.get("invoice.due.date",
                    dueDate.format(formatter));
        }

        if (issueDate != null) {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            localizedIssueDate = messageService.get("invoice.issue.date",
                    issueDate.format(formatter));
        }
    }

    /**
     * Formats balance for display
     */
    private String formatBalance(BigDecimal balance) {
        if (balance == null) return "0.00";
        return String.format("%,.2f", balance);
    }
}