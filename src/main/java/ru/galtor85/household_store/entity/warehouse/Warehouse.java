package ru.galtor85.household_store.entity.warehouse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses", schema = "household_schema")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Код склада (например, "WH-001")

    @Column(nullable = false)
    private String name; // Название склада

    private String description; // Описание

    @Column(nullable = false, unique = true)
    private String barcode; // Штрих-код склада

    @Column(name = "barcode_format")
    private String barcodeFormat; // EAN_13, CODE_128, QR_CODE и т.д.

    @Column(nullable = false)
    private String address; // Физический адрес склада

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "total_capacity")
    private Integer totalCapacity; // Общая вместимость в ячейках

    @Column(name = "used_capacity")
    private Integer usedCapacity; // Занято ячеек

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Связь с ячейками
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StorageCell> cells = new ArrayList<>();

    // Helper methods
    public void addCell(StorageCell cell) {
        cells.add(cell);
        cell.setWarehouse(this);
    }

    public void removeCell(StorageCell cell) {
        cells.remove(cell);
        cell.setWarehouse(null);
    }
}