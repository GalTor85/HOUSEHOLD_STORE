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
 * Entity representing a bank account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bank_accounts", schema = "household_schema")
public class BankAccount {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String name;

    // =========================================================================
    // BANK DETAILS
    // =========================================================================

    @Column(nullable = false)
    private String bankName;

    private String bic;

    @Column(name = "correspondent_account")
    private String correspondentAccount;

    private String iban;

    @Column(name = "swift_code")
    private String swiftCode;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "RUB";

    /**
     * -- GETTER --
     *  Checks if the account is active
     */
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    // =========================================================================
    // ACCOUNT TYPE
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private BankAccountType accountType = BankAccountType.CHECKING;

    // =========================================================================
    // AUDIT FIELDS
    // =========================================================================

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================

    /**
     * Deposits money to the bank account
     *
     * @param amount amount to deposit
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Withdraws money from the bank account
     *
     * @param amount amount to withdraw
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }
}