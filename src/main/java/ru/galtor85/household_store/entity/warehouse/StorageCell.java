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
    private String code;

    @Column(nullable = false, unique = true)
    private String barcode;

    @Column(name = "barcode_format")
    private String barcodeFormat;

    @Column(name = "section")
    private String section;

    @Column(name = "rack")
    private String rack;

    @Column(name = "shelf")
    private String shelf;

    @Column(name = "position")
    private String position;

    @Column(name = "cell_type")
    @Enumerated(EnumType.STRING)
    private CellType cellType;

    @Column(name = "max_weight_kg")
    private Double maxWeightKg;

    @Column(name = "max_volume_m3")
    private Double maxVolumeM3;

    @Column(name = "current_product_id")
    private Long currentProductId;

    @Column(name = "current_quantity")
    private Integer currentQuantity;

    @Column(name = "is_occupied")
    private Boolean isOccupied;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_inventory_date")
    private LocalDateTime lastInventoryDate;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}