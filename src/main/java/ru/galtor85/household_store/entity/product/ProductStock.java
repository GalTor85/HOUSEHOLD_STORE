package ru.galtor85.household_store.entity.product;

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
    private Integer reservedQuantity; // Reserved for orders

    @Column(name = "available_quantity")
    private Integer availableQuantity; // Available for sale

    @Column(name = "min_stock_level")
    private Integer minStockLevel; // Minimum stock level

    @Column(name = "max_stock_level")
    private Integer maxStockLevel; // Maximum stock level

    @Column(name = "reorder_point")
    private Integer reorderPoint; // Reorder point

    @Column(name = "location_in_warehouse")
    private String locationInWarehouse; // Location in warehouse (row/rack/shelf)

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}