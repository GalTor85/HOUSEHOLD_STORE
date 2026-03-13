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
@Table(name = "price_rules", schema = "household_schema")
public class PriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Название правила

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; // Значение скидки

    @Column(name = "min_quantity")
    private Integer minQuantity; // Минимальное количество для скидки

    @Column(name = "max_quantity")
    private Integer maxQuantity; // Максимальное количество

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // Минимальная сумма заказа

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ElementCollection
    @CollectionTable(name = "price_rule_user_types",
            schema = "household_schema",
            joinColumns = @JoinColumn(name = "price_rule_id"))
    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private Set<UserType> applicableUserTypes = new HashSet<>(); // Для каких типов пользователей

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "priority")
    private Integer priority; // Приоритет применения (меньше = выше)

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isApplicableForUser(UserType userType) {
        return applicableUserTypes.isEmpty() || applicableUserTypes.contains(userType);
    }

    public boolean isValidNow() {
        LocalDateTime now = LocalDateTime.now();
        return (startDate == null || now.isAfter(startDate)) &&
                (endDate == null || now.isBefore(endDate));
    }
}