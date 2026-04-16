package ru.galtor85.household_store.entity.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_items", schema = "household_schema")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "supplier_price", precision = 10, scale = 2)
    private BigDecimal supplierPrice;

    @Column(name = "supplier_sku")
    private String supplierSku;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    public int getRemainingQuantity() {
        if (receivedQuantity == null) {
            return quantity;
        }
        return quantity - receivedQuantity;
    }
}