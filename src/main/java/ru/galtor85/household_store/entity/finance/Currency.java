package ru.galtor85.household_store.entity.finance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "currencies", schema = "household_schema")
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 3)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol", nullable = false, length = 5)
    private String symbol;

    @Column(name = "is_base")
    @Builder.Default
    private Boolean isBase = false;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "decimal_places")
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public BigDecimal convertToBase(BigDecimal amount) {
        if (exchangeRate == null || amount == null) {
            return amount;
        }
        return amount.multiply(exchangeRate);
    }

    public BigDecimal convertFromBase(BigDecimal amount) {
        if (exchangeRate == null || amount == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.divide(exchangeRate, decimalPlaces, RoundingMode.HALF_UP);
    }
}