package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@ToString(exclude = {"order"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items", schema = "household_schema")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_product_id")
    private Long supplierProductId; // Для закупок

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Цена на момент заказа

    @Column(name = "supplier_price", precision = 10, scale = 2)
    private BigDecimal supplierPrice; // Закупочная цена

    @Column(name = "product_name")
    private String productName; // Название на момент заказа

    @Column(name = "product_sku")
    private String productSku; // Артикул на момент заказа

    @Column(name = "supplier_sku")
    private String supplierSku; // Артикул поставщика

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice; // Итоговая цена позиции

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        this.totalPrice = this.price
                .multiply(BigDecimal.valueOf(this.quantity))
                .subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }
}