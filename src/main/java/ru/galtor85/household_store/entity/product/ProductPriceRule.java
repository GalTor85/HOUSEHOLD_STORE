package ru.galtor85.household_store.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_price_rules", schema = "household_schema")
public class ProductPriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "price_rule_id", nullable = false)
    private Long priceRuleId;

    @Column(name = "is_excluded")
    private boolean excluded; // Исключение из правила
}