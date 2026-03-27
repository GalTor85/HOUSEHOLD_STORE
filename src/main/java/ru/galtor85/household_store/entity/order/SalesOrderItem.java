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
    private BigDecimal price; // Цена на момент заказа

    @Column(name = "product_name")
    private String productName; // Название на момент заказа

    @Column(name = "product_sku")
    private String productSku; // Артикул на момент заказа

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice; // Итоговая цена позиции

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Рассчитывает итоговую цену позиции
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

    /**
     * Получает цену со скидкой
     */
    public BigDecimal getDiscountedPrice() {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (discountAmount != null) {
            return price.subtract(discountAmount.divide(BigDecimal.valueOf(quantity),
                    BigDecimal.ROUND_HALF_UP));
        }
        return price;
    }

    /**
     * Получает сумму скидки на позицию
     */
    public BigDecimal getTotalDiscount() {
        if (discountAmount != null) {
            return discountAmount;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Проверяет, есть ли скидка на позицию
     */
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}