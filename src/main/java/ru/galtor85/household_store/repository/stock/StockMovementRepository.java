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
import java.util.Optional;

/**
 * Repository for StockMovement entity
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // =========================================================================
    // SEARCH BY PRODUCT
    // =========================================================================

    /**
     * Finds all stock movements for a specific product
     *
     * @param productId product identifier
     * @return list of stock movements
     */
    List<StockMovement> findByProductId(Long productId);

    /**
     * Finds stock movements for a product with pagination
     *
     * @param productId product identifier
     * @param pageable  pagination information
     * @return page of stock movements
     */
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    /**
     * Finds latest stock movements for a product
     *
     * @param productId product identifier
     * @param pageable  pagination information
     * @return list of latest movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.productId = :productId ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestByProductId(@Param("productId") Long productId, Pageable pageable);

    // =========================================================================
    // SEARCH BY WAREHOUSE
    // =========================================================================

    /**
     * Finds all stock movements for a specific warehouse
     *
     * @param warehouseId warehouse identifier
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId")
    List<StockMovement> findByWarehouseId(@Param("warehouseId") Long warehouseId);

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
     * Finds stock movements related to a cell with pagination
     *
     * @param cellId   cell identifier
     * @param pageable pagination information
     * @return page of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    Page<StockMovement> findByCellId(@Param("cellId") Long cellId, Pageable pageable);

    /**
     * Finds movements from a specific source cell
     *
     * @param fromCellId source cell identifier
     * @return list of stock movements
     */
    List<StockMovement> findByFromCellId(Long fromCellId);

    /**
     * Finds movements to a specific destination cell
     *
     * @param toCellId destination cell identifier
     * @return list of stock movements
     */
    List<StockMovement> findByToCellId(Long toCellId);

    // =========================================================================
    // SEARCH BY DATE RANGE
    // =========================================================================

    /**
     * Finds product movements within a date range
     *
     * @param productId product identifier
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findProductMovementsInPeriod(@Param("productId") Long productId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Finds warehouse movements within a date range
     *
     * @param warehouseId warehouse identifier
     * @param startDate   start date (inclusive)
     * @param endDate     end date (inclusive)
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.warehouseId = :warehouseId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findWarehouseMovementsInPeriod(@Param("warehouseId") Long warehouseId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // AGGREGATION QUERIES
    // =========================================================================

    /**
     * Sums movement quantities by type for a product within a date range
     *
     * @param productId    product identifier
     * @param movementType movement type (RECEIPT, SHIPMENT, etc.)
     * @param startDate    start date (inclusive)
     * @param endDate      end date (inclusive)
     * @return total quantity summed
     */
    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.movementType = :movementType AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    Integer sumMovementsByType(@Param("productId") Long productId,
                               @Param("movementType") MovementType movementType,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Summarizes movements by type for a warehouse within a date range
     *
     * @param warehouseId warehouse identifier
     * @param startDate   start date (inclusive)
     * @param endDate     end date (inclusive)
     * @return list of [movementType, totalQuantity] objects
     */
    @Query("SELECT sm.movementType, SUM(sm.quantity) FROM StockMovement sm WHERE " +
            "sm.warehouseId = :warehouseId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.movementType")
    List<Object[]> summarizeByType(@Param("warehouseId") Long warehouseId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // SEARCH BY REFERENCE
    // =========================================================================

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

    /**
     * Finds movements by reference type and reference number
     *
     * @param refType      reference type (ORDER, PURCHASE, WRITEOFF, INVENTORY)
     * @param refNumber    reference number
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :refType AND sm.referenceNumber = :refNumber")
    List<StockMovement> findByReferenceNumber(@Param("refType") String refType,
                                              @Param("refNumber") String refNumber);

    /**
     * Finds movements by reference type, reference ID, and product ID
     *
     * @param referenceType reference type
     * @param referenceId   reference identifier
     * @param productId     product identifier
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :referenceType AND " +
            "sm.referenceId = :referenceId AND " +
            "sm.productId = :productId")
    List<StockMovement> findByReferenceAndProductId(@Param("referenceType") String referenceType,
                                                    @Param("referenceId") Long referenceId,
                                                    @Param("productId") Long productId);

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

    /**
     * Finds movements by batch number and product ID
     *
     * @param batchNumber batch/lot number
     * @param productId   product identifier
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND sm.productId = :productId")
    List<StockMovement> findByBatchAndProduct(@Param("batchNumber") String batchNumber,
                                              @Param("productId") Long productId);

    // =========================================================================
    // LATEST MOVEMENTS
    // =========================================================================

    /**
     * Finds the latest stock movements across all warehouses
     *
     * @param pageable pagination information
     * @return list of latest movements
     */
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestMovements(Pageable pageable);

    /**
     * Finds the latest movements for a specific warehouse
     *
     * @param warehouseId warehouse identifier
     * @param pageable    pagination information
     * @return list of latest warehouse movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestWarehouseMovements(@Param("warehouseId") Long warehouseId, Pageable pageable);

    /**
     * Finds the latest movements for a specific batch and product
     *
     * @param batchNumber batch/lot number
     * @param productId   product identifier
     * @param pageable    pagination information
     * @return list of latest batch movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND sm.productId = :productId " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestByBatchAndProduct(@Param("batchNumber") String batchNumber,
                                                    @Param("productId") Long productId,
                                                    Pageable pageable);

    // =========================================================================
    // CELL MOVEMENTS
    // =========================================================================

    /**
     * Finds all movements related to a cell (as source or destination)
     *
     * @param fromCellId source cell identifier
     * @param toCellId   destination cell identifier
     * @return list of stock movements
     */
    List<StockMovement> findByFromCellIdOrToCellId(Long fromCellId, Long toCellId);

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

    /**
     * Finds movements by batch number within a date range
     *
     * @param batchNumber batch/lot number
     * @param startDate   start date (inclusive)
     * @param endDate     end date (inclusive)
     * @return list of stock movements
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByBatchInPeriod(@Param("batchNumber") String batchNumber,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

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
     * Finds receipt transaction for a specific product in a purchase order
     *
     * @param orderId   purchase order identifier
     * @param productId product identifier
     * @return optional receipt transaction
     */
    @Query("SELECT sm FROM StockMovement sm " +
            "WHERE sm.referenceType = 'PURCHASE' " +
            "AND sm.referenceId = :orderId " +
            "AND sm.productId = :productId " +
            "AND sm.movementType = 'RECEIPT'")
    Optional<StockMovement> findReceiptTransactionByOrderAndProduct(
            @Param("orderId") Long orderId,
            @Param("productId") Long productId);
}