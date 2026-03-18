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
@Table(name = "category_warehouse", schema = "household_schema")
public class CategoryWarehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String category; // Категория товара

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId; // ID склада для этой категории

    @Column(name = "is_default")
    private Boolean isDefault; // Склад по умолчанию для этой категории

    @Column(name = "priority")
    private Integer priority; // Приоритет (если несколько складов)
}