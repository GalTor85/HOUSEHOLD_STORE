package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promo_code_usages", schema = "household_schema")
public class PromoCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promo_code_id", nullable = false)
    private Long promoCodeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId; // ID заказа, где использован

    @Column(name = "used_at")
    @CreationTimestamp
    private LocalDateTime usedAt;
}