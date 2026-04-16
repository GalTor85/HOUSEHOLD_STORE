package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.finance.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cash transaction DTO")
public class CashTransactionDto {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Schema(description = "Transaction ID", example = "1")
    private Long id;

    @Schema(description = "Cash register ID", example = "1")
    private Long cashRegisterId;

    @Schema(description = "Cash register name", example = "Основная касса")
    private String cashRegisterName;

    @Schema(description = "Cash register number", example = "REG-001")
    private String cashRegisterNumber;

    // =========================================================================
    // INVOICE DETAILS
    // =========================================================================

    @Schema(description = "Invoice ID", example = "1")
    private Long invoiceId;

    @Schema(description = "Invoice number", example = "INV-20240325-001")
    private String invoiceNumber;

    // =========================================================================
    // TRANSACTION DETAILS
    // =========================================================================

    @Schema(description = "Transaction type", example = "INCOME")
    private TransactionType transactionType;

    @Schema(description = "Localized transaction type", example = "Приход")
    private String localizedTransactionType;

    @Schema(description = "Transaction amount", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "RUB", defaultValue = "RUB")
    @Builder.Default
    private String currency = "RUB";

    // =========================================================================
    // PAYMENT METHOD DETAILS
    // =========================================================================

    @Schema(description = "Payment method", example = "CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Localized payment method", example = "Банковская карта")
    private String localizedPaymentMethod;

    // =========================================================================
    // CASHIER DETAILS
    // =========================================================================

    @Schema(description = "Cashier ID", example = "1")
    private Long cashierId;

    @Schema(description = "Cashier name", example = "Иванов Иван")
    private String cashierName;

    @Schema(description = "Cashier email", example = "ivan@example.com")
    private String cashierEmail;

    // =========================================================================
    // ADDITIONAL DETAILS
    // =========================================================================

    @Schema(description = "Transaction description", example = "Оплата счета INV-001")
    private String description;

    @Schema(description = "Notes", example = "Клиент оплатил картой")
    private String notes;

    // =========================================================================
    // DATE DETAILS
    // =========================================================================

    @Schema(description = "Transaction timestamp")
    private LocalDateTime createdAt;

    // =========================================================================
    // UI DETAILS
    // =========================================================================

    @Schema(description = "Sign for amount display (+/-)", example = "+")
    private String sign;

    @Schema(description = "Color for UI (HEX)", example = "#28a745")
    private String color;

    @Schema(description = "Icon name for UI", example = "trending-up")
    private String icon;

    // =========================================================================
    // BALANCE DETAILS
    // =========================================================================

    @Schema(description = "Cash register balance before transaction")
    private BigDecimal balanceBefore;

    @Schema(description = "Cash register balance after transaction")
    private BigDecimal balanceAfter;
}