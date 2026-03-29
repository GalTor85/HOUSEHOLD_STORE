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
    // ИДЕНТИФИКАТОРЫ
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
    // СВЯЗЬ С ИНВОЙСОМ
    // =========================================================================

    @Schema(description = "Invoice ID", example = "1")
    private Long invoiceId;

    @Schema(description = "Invoice number", example = "INV-20240325-001")
    private String invoiceNumber;

    // =========================================================================
    // ТИП И СУММА ОПЕРАЦИИ
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
    // СПОСОБ ОПЛАТЫ
    // =========================================================================

    @Schema(description = "Payment method", example = "CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Localized payment method", example = "Банковская карта")
    private String localizedPaymentMethod;

    // =========================================================================
    // КАССИР
    // =========================================================================

    @Schema(description = "Cashier ID", example = "1")
    private Long cashierId;

    @Schema(description = "Cashier name", example = "Иванов Иван")
    private String cashierName;

    @Schema(description = "Cashier email", example = "ivan@example.com")
    private String cashierEmail;

    // =========================================================================
    // ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
    // =========================================================================

    @Schema(description = "Transaction description", example = "Оплата счета INV-001")
    private String description;

    @Schema(description = "Notes", example = "Клиент оплатил картой")
    private String notes;

    // =========================================================================
    // ДАТЫ
    // =========================================================================

    @Schema(description = "Transaction timestamp")
    private LocalDateTime createdAt;

    // =========================================================================
    // UI ПОЛЯ (для отображения)
    // =========================================================================

    @Schema(description = "Sign for amount display (+/-)", example = "+")
    private String sign;

    @Schema(description = "Color for UI (HEX)", example = "#28a745")
    private String color;

    @Schema(description = "Icon name for UI", example = "trending-up")
    private String icon;

    // =========================================================================
    // БАЛАНСОВЫЕ ПОЛЯ
    // =========================================================================

    @Schema(description = "Cash register balance before transaction")
    private BigDecimal balanceBefore;

    @Schema(description = "Cash register balance after transaction")
    private BigDecimal balanceAfter;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Проверяет, является ли операция приходом
     */
    public boolean isIncome() {
        return transactionType == TransactionType.INCOME;
    }

    /**
     * Проверяет, является ли операция расходом
     */
    public boolean isExpense() {
        return transactionType == TransactionType.EXPENSE;
    }

    /**
     * Проверяет, является ли операция возвратом
     */
    public boolean isRefund() {
        return transactionType == TransactionType.REFUND;
    }

    /**
     * Проверяет, является ли операция наличной
     */
    public boolean isCash() {
        return paymentMethod == PaymentMethod.CASH;
    }

    /**
     * Получает форматированную сумму со знаком
     */
    public String getFormattedAmount() {
        return sign != null ? sign + amount.toString() : amount.toString();
    }

    /**
     * Получает сумму в абсолютном значении
     */
    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }
}