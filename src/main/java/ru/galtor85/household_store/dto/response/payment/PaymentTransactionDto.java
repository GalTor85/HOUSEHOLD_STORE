package ru.galtor85.household_store.dto.response.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment transaction response DTO")
public class PaymentTransactionDto {

    @Schema(description = "Transaction ID", example = "1")
    private Long id;

    @Schema(description = "Payment method ID", example = "1")
    private Long paymentMethodId;

    @Schema(description = "Invoice ID", example = "123")
    private Long invoiceId;

    @Schema(description = "Order ID", example = "456")
    private Long orderId;

    @Schema(description = "Order type", example = "PURCHASE")
    private String orderType;

    @Schema(description = "Transaction amount", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "RUB")
    private String currency;

    @Schema(description = "Transaction status", example = "COMPLETED")
    private PaymentTransactionStatus status;

    @Schema(description = "Localized status", example = "Completed")
    private String localizedStatus;

    @Schema(description = "Provider transaction ID", example = "TXN-123456")
    private String providerTransactionId;

    @Schema(description = "Payment URL", example = "https://pay.example.com/123")
    private String providerPaymentUrl;

    @Schema(description = "Description", example = "Payment for invoice INV-001")
    private String description;

    @Schema(description = "Processing fee", example = "75.00")
    private BigDecimal processingFee;

    @Schema(description = "Net amount", example = "4925.00")
    private BigDecimal netAmount;

    @Schema(description = "Completed at timestamp")
    private LocalDateTime completedAt;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;
}