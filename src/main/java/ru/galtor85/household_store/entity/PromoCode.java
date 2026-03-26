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
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promo_codes", schema = "household_schema")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_uses")
    private Integer maxUses; // Максимальное количество использований

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit; // Лимит на одного пользователя

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ElementCollection
    @CollectionTable(name = "promo_code_user_types",
            schema = "household_schema",
            joinColumns = @JoinColumn(name = "promo_code_id"))
    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserType> applicableUserTypes = new HashSet<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_combined")
    @Builder.Default
    private boolean combined = false; // Можно ли комбинировать с другими скидками

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isValid() {
        if (!active) return false;
        if (maxUses != null && usedCount >= maxUses) return false;

        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        return true;
    }

    public void incrementUsed() {
        usedCount++;
    }
}