package ru.galtor85.household_store.repository.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.stock.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for StockMovement entity
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // =========================================================================
    // SEARCH BY PRODUCT
    // =========================================================================

    /**
     * Finds stock movements for a product with pagination
     *
     * @param productId product identifier
     * @param pageable  pagination information
     * @return page of stock movements
     */
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    // =========================================================================
    // SEARCH BY WAREHOUSE
    // =========================================================================

    /**
     * Finds stock movements for a warehouse with pagination
     *
     * @param warehouseId warehouse identifier
     * @param pageable    pagination information
     * @return page of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId")
    Page<StockMovement> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

    // =========================================================================
    // SEARCH BY CELL
    // =========================================================================

    /**
     * Finds all stock movements related to a specific cell (as source or destination)
     *
     * @param cellId cell identifier
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    List<StockMovement> findByCellId(@Param("cellId") Long cellId);

    /**
     * Finds movements by reference type and reference ID
     *
     * @param refType reference type (ORDER, PURCHASE, WRITEOFF, INVENTORY)
     * @param refId   reference identifier
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :refType AND sm.referenceId = :refId")
    List<StockMovement> findByReference(@Param("refType") String refType,
                                        @Param("refId") Long refId);

    // =========================================================================
    // SEARCH BY BATCH
    // =========================================================================

    /**
     * Finds movements by batch number
     *
     * @param batchNumber batch/lot number
     * @return list of stock movements
     */
    List<StockMovement> findByBatchNumber(String batchNumber);

    // =========================================================================
    // BATCH NUMBERS
    // =========================================================================

    /**
     * Finds all distinct batch numbers for a product
     *
     * @param productId product identifier
     * @return list of batch numbers
     */
    @Query("SELECT DISTINCT sm.batchNumber FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND sm.batchNumber IS NOT NULL")
    List<String> findBatchNumbersByProduct(@Param("productId") Long productId);

    // =========================================================================
    // RECEIPT TRANSACTIONS
    // =========================================================================

    /**
     * Finds all receipt transactions for a purchase order
     *
     * @param orderId purchase order identifier
     * @return list of receipt stock movements
     */
    @Query("SELECT sm FROM StockMovement sm " +
            "WHERE sm.referenceType = 'PURCHASE' " +
            "AND sm.referenceId = :orderId " +
            "AND sm.movementType = 'RECEIPT' " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findReceiptTransactionsByOrderId(@Param("orderId") Long orderId);

    /**
     * Checks if there are any stock movements for a given cell
     *
     * @param cellId cell identifier
     * @return true if there are movements, false otherwise
     */
    @Query("SELECT COUNT(sm) > 0 FROM StockMovement sm " +
            "WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    boolean existsByFromCellIdOrToCellId(@Param("cellId") Long cellId);

    /**
     * Filters stock movements by multiple criteria.
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "(:productId IS NULL OR sm.productId = :productId) AND " +
            "(:warehouseId IS NULL OR sm.warehouseId = :warehouseId) AND " +
            "(:cellId IS NULL OR sm.fromCellId = :cellId OR sm.toCellId = :cellId) AND " +
            "(:movementType IS NULL OR sm.movementType = :movementType) AND " +
            "(:referenceType IS NULL OR sm.referenceType = :referenceType) AND " +
            "(:referenceId IS NULL OR sm.referenceId = :referenceId) AND " +
            "(:batchNumber IS NULL OR sm.batchNumber = :batchNumber) AND " +
            "(:startDate IS NULL OR sm.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR sm.createdAt <= :endDate)")
    Page<StockMovement> filter(@Param("productId") Long productId,
                               @Param("warehouseId") Long warehouseId,
                               @Param("cellId") Long cellId,
                               @Param("movementType") MovementType movementType,
                               @Param("referenceType") String referenceType,
                               @Param("referenceId") Long referenceId,
                               @Param("batchNumber") String batchNumber,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate,
                               Pageable pageable);

    /**
     * Gets summary of stock movements for a period.
     */
    @Query("SELECT COUNT(sm), " +
            "SUM(CASE WHEN sm.movementType = 'RECEIPT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'SHIPMENT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'TRANSFER' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'WRITE_OFF' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'RETURN' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'RECEIPT' THEN sm.quantity ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'SHIPMENT' THEN sm.quantity ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'TRANSFER' THEN sm.quantity ELSE 0 END), " +
            "SUM(CASE WHEN sm.movementType = 'WRITE_OFF' THEN sm.quantity ELSE 0 END) " +
            "FROM StockMovement sm " +
            "WHERE (:warehouseId IS NULL OR sm.warehouseId = :warehouseId) " +
            "AND (:productId IS NULL OR sm.productId = :productId) " +
            "AND sm.createdAt BETWEEN :startDate AND :endDate")
    Object[] getSummary(@Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}