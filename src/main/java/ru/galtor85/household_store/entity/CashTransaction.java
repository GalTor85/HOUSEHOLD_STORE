package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cash_transactions", schema = "household_schema")
public class CashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "RUB";  // ← ДОБАВИТЬ ЭТО ПОЛЕ

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String notes;

    @Column(name = "original_transaction_id")
    private Long originalTransactionId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

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
     * Получает сумму со знаком (+ для прихода, - для расхода)
     */
    public BigDecimal getSignedAmount() {
        if (isExpense()) {
            return amount.negate();
        }
        return amount;
    }

    /**
     * Получает локализованное название типа операции
     */
    public String getLocalizedTransactionType(ru.galtor85.household_store.service.MessageService messageService) {
        return transactionType.getLocalizedName(messageService);
    }

    /**
     * Получает локализованное название способа оплаты
     */
    public String getLocalizedPaymentMethod(ru.galtor85.household_store.service.MessageService messageService) {
        return paymentMethod != null ? paymentMethod.getLocalizedName(messageService) : null;
    }
}