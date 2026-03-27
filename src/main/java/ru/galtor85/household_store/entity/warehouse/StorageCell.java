package ru.galtor85.household_store.entity.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_cells", schema = "household_schema")
public class StorageCell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private String code; // Код ячейки (например, "A-01-01")

    @Column(nullable = false, unique = true)
    private String barcode; // Штрих-код ячейки

    @Column(name = "barcode_format")
    private String barcodeFormat; // EAN_13, CODE_128, QR_CODE

    @Column(name = "section")
    private String section; // Секция (A, B, C...)

    @Column(name = "rack")
    private String rack; // Стеллаж

    @Column(name = "shelf")
    private String shelf; // Полка

    @Column(name = "position")
    private String position; // Позиция на полке

    @Column(name = "cell_type")
    @Enumerated(EnumType.STRING)
    private CellType cellType; // STANDARD, PALLET, FRIDGE, DANGEROUS и т.д.

    @Column(name = "max_weight_kg")
    private Double maxWeightKg; // Максимальный вес в кг

    @Column(name = "max_volume_m3")
    private Double maxVolumeM3; // Максимальный объем в м³

    @Column(name = "current_product_id")
    private Long currentProductId; // ID товара в ячейке

    @Column(name = "current_quantity")
    private Integer currentQuantity; // Текущее количество

    @Column(name = "is_occupied")
    private Boolean isOccupied;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_inventory_date")
    private LocalDateTime lastInventoryDate; // Дата последней инвентаризации

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}