package ru.galtor85.household_store.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for order payment summary information.
 * Contains aggregated payment status and next payable invoice details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment summary for an order")
public class PaymentSummaryDto {

    @Schema(description = "Total paid amount", example = "2000.00")
    private BigDecimal totalPaid;

    @Schema(description = "Remaining amount to pay", example = "3000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "Has payable invoices", example = "true")
    private boolean hasPayableInvoices;

    @Schema(description = "Next invoice number to pay", example = "INV-20240330-001")
    private String nextInvoiceNumber;

    @Schema(description = "Invoice status", example = "PENDING")
    private String invoiceStatus;

    @Schema(description = "Payment URL for the next invoice", example = "/app/users/invoices/INV-20240330-001/pay")
    private String paymentUrl;

    @Schema(description = "Localized total paid amount", example = "Paid: 2,000.00 RUB")
    private String localizedTotalPaid;

    @Schema(description = "Localized remaining amount", example = "Remaining: 3,000.00 RUB")
    private String localizedRemainingAmount;

    @Schema(description = "Localized payment status", example = "Order fully paid")
    private String localizedPaymentStatus;

    @Schema(description = "Localized action button text", example = "Pay Now")
    private String localizedActionText;
}