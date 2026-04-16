package ru.galtor85.household_store.entity.warehouse;

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
    private String category;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "priority")
    private Integer priority;
}