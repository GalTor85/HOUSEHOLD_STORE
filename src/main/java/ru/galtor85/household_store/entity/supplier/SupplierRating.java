package ru.galtor85.household_store.entity.supplier;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier_ratings", schema = "household_schema")
public class SupplierRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Кто оставил отзыв

    @Column(nullable = false)
    private Integer rating; // Оценка от 1 до 5

    @Column(length = 1000)
    private String comment; // Текст отзыва

    @Column(name = "order_id")
    private Long orderId; // ID заказа (для проверки)

    @Column(name = "is_verified")
    private Boolean verified; // Подтвержденный отзыв

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}