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

/**
 * Entity representing a warehouse in the system.
 *
 * <p>Warehouse contains storage cells and manages inventory. Each warehouse
 * has a unique code and barcode for identification. Warehouses can be
 * hidden from customer view using isVisibleForSale flag.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
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
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, unique = true)
    private String barcode;

    @Column(name = "barcode_format")
    private String barcodeFormat;

    @Column(nullable = false)
    private String address;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Column(name = "used_capacity")
    private Integer usedCapacity;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StorageCell> cells = new ArrayList<>();

    @Column(name = "is_visible_for_sale")
    @Builder.Default
    private Boolean isVisibleForSale = true;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Adds a storage cell to the warehouse.
     *
     * @param cell the storage cell to add
     */
    public void addCell(StorageCell cell) {
        cells.add(cell);
        cell.setWarehouse(this);
    }

    /**
     * Removes a storage cell from the warehouse.
     *
     * @param cell the storage cell to remove
     */
    public void removeCell(StorageCell cell) {
        cells.remove(cell);
        cell.setWarehouse(null);
    }

    /**
     * Checks if warehouse is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Checks if warehouse is visible for sale to customers.
     *
     * @return true if visible for sale
     */
    public boolean isVisibleForSale() {
        return Boolean.TRUE.equals(isVisibleForSale);
    }

    /**
     * Gets available capacity (total - used).
     *
     * @return available capacity
     */
    public int getAvailableCapacity() {
        if (totalCapacity == null) {
            return 0;
        }
        int used = usedCapacity != null ? usedCapacity : 0;
        return totalCapacity - used;
    }

    /**
     * Gets occupancy percentage.
     *
     * @return occupancy percentage (0-100)
     */
    public double getOccupancyPercentage() {
        if (totalCapacity == null || totalCapacity == 0) {
            return 0.0;
        }
        int used = usedCapacity != null ? usedCapacity : 0;
        return (used * 100.0) / totalCapacity;
    }
}