package ru.galtor85.household_store.repository.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.stock.StockMovement;

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

    @Query("SELECT COUNT(sm) > 0 FROM StockMovement sm " +
            "WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    boolean existsByFromCellIdOrToCellId(@Param("cellId") Long cellId);
}