package ru.galtor85.household_store.entity.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing stock movement (receipt, shipment, transfer, write-off, return)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_movements", schema = "household_schema")
public class StockMovement {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    // =========================================================================
    // CELL REFERENCES
    // =========================================================================

    @Column(name = "from_cell_id")
    private Long fromCellId;

    @Column(name = "to_cell_id")
    private Long toCellId;

    // =========================================================================
    // MOVEMENT DETAILS
    // =========================================================================

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "movement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    // =========================================================================
    // REFERENCE INFORMATION
    // =========================================================================

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "reference_type")
    private String referenceType; // ORDER, PURCHASE, WRITEOFF, INVENTORY

    @Column(name = "reference_id")
    private Long referenceId;

    // =========================================================================
    // TRACKING INFORMATION
    // =========================================================================

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "document_number")
    private String documentNumber;

    // =========================================================================
    // AUDIT FIELDS
    // =========================================================================

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "notes")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // =========================================================================
    // RETURN INFORMATION
    // =========================================================================

    @Column(name = "original_movement_id")
    private Long originalMovementId;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity;
}