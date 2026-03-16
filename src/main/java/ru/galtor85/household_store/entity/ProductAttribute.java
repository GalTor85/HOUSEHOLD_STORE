package ru.galtor85.household_store.entity;

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
@Table(name = "product_attributes", schema = "household_schema")
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name; // Название характеристики (например, "Цвет", "Размер", "Вес")

    @Column(nullable = false)
    private String value; // Значение характеристики (например, "Красный", "XL", "1.5 кг")

    @Column(name = "attribute_order")
    private Integer order; // Порядок отображения

    @Column(name = "is_required")
    private Boolean required; // Обязательная ли характеристика

    @Column(name = "is_variant")
    private Boolean variant; // Является ли вариантом для выбора (для вариативных товаров)
}