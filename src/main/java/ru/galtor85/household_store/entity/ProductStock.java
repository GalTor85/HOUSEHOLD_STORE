package ru.galtor85.household_store.entity;

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
@Table(name = "product_stocks", schema = "household_schema",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity; // Зарезервировано для заказов

    @Column(name = "available_quantity")
    private Integer availableQuantity; // Доступно для продажи

    @Column(name = "min_stock_level")
    private Integer minStockLevel; // Минимальный уровень запаса

    @Column(name = "max_stock_level")
    private Integer maxStockLevel; // Максимальный уровень запаса

    @Column(name = "reorder_point")
    private Integer reorderPoint; // Точка заказа

    @Column(name = "location_in_warehouse")
    private String locationInWarehouse; // Местоположение на складе (ряд/стеллаж/полка)

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}