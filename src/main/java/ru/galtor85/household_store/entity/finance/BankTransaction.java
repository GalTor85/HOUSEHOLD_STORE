package ru.galtor85.household_store.entity.finance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing bank account transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bank_transactions", schema = "household_schema")
public class BankTransaction {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    // =========================================================================
    // TRANSACTION TYPE
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private BankTransactionType type;

    // =========================================================================
    // AMOUNTS
    // =========================================================================

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    // =========================================================================
    // REFERENCE INFORMATION
    // =========================================================================

    @Column(length = 500)
    private String description;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    // =========================================================================
    // TRANSFER INFORMATION (for TRANSFER type)
    // =========================================================================

    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id")
    private Long toAccountId;

    // =========================================================================
    // TIMESTAMPS
    // =========================================================================

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Checks if transaction is a deposit
     */
    public boolean isDeposit() {
        return type == BankTransactionType.DEPOSIT;
    }

    /**
     * Checks if transaction is a withdrawal
     */
    public boolean isWithdraw() {
        return type == BankTransactionType.WITHDRAW;
    }

    /**
     * Checks if transaction is a transfer
     */
    public boolean isTransfer() {
        return type == BankTransactionType.TRANSFER;
    }

    /**
     * Gets the net change to account balance
     */
    public BigDecimal getNetChange() {
        if (balanceAfter != null && balanceBefore != null) {
            return balanceAfter.subtract(balanceBefore);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets formatted amount with sign
     */
    public String getFormattedAmount() {
        if (amount == null) return "0.00";
        if (isDeposit()) {
            return "+" + amount.toString();
        } else if (isWithdraw()) {
            return "-" + amount.toString();
        }
        return amount.toString();
    }
}