package ru.galtor85.household_store.entity.supplier;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier_products", schema = "household_schema",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"supplier_id", "product_id"})
        })
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_price", precision = 10, scale = 2)
    private BigDecimal supplierPrice;

    @Column(name = "supplier_sku")
    private String supplierSku;

    @Column(name = "is_main_supplier")
    private Boolean mainSupplier;

    @Column(name = "delivery_time")
    private Integer deliveryTime;

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}