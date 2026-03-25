package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cash_registers", schema = "household_schema")
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "register_number", nullable = false, unique = true)
    private String registerNumber;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "opening_balance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 10, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_by")  // ← ДОБАВИТЬ ЭТО ПОЛЕ
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CashTransaction> transactions = new ArrayList<>();

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public BigDecimal getCurrentBalance() {
        if (transactions == null || transactions.isEmpty()) {
            return openingBalance;
        }
        return transactions.stream()
                .map(t -> t.getTransactionType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(openingBalance, BigDecimal::add);
    }

    public void close(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
        this.isActive = false;
        this.closedAt = LocalDateTime.now();
    }

    public void addTransaction(CashTransaction transaction) {
        transactions.add(transaction);
        transaction.setCashRegister(this);
    }

    public boolean isOpen() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isClosed() {
        return !isOpen();
    }
}