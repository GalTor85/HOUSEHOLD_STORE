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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "carts", schema = "household_schema",
        indexes = {
                @Index(name = "idx_carts_user_id", columnList = "user_id"),
                @Index(name = "idx_carts_status", columnList = "status")
        })
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // ID пользователя (связь по ID, как и везде)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "items_count")
    private Integer itemsCount;

    @Column(name = "category")
    private String category; // Категория корзины (для истории)

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Время истечения корзины

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        recalculateTotal();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.itemsCount = items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void clear() {
        items.clear();
        this.totalAmount = BigDecimal.ZERO;
        this.itemsCount = 0;
    }
}