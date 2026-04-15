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
@Table(name = "sales_order_items", schema = "household_schema")
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price at order time

    @Column(name = "product_name")
    private String productName; // Name at order time

    @Column(name = "product_sku")
    private String productSku; // SKU at order time

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice; // Final item price

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Calculates the total price of the item.
     */
    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (price != null && quantity != null) {
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            if (discountAmount != null) {
                this.totalPrice = subtotal.subtract(discountAmount);
            } else {
                this.totalPrice = subtotal;
            }
        }
    }
}