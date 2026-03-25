package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.MovementType;
import ru.galtor85.household_store.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // ========== ПОИСК ПО ПРОДУКТУ ==========

    List<StockMovement> findByProductId(Long productId);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.productId = :productId ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestByProductId(@Param("productId") Long productId, Pageable pageable);

    // ========== ПОИСК ПО СКЛАДУ ==========

    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId")
    List<StockMovement> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId")
    Page<StockMovement> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

    // ========== ПОИСК ПО ЯЧЕЙКАМ ==========

    @Query("SELECT sm FROM StockMovement sm WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    List<StockMovement> findByCellId(@Param("cellId") Long cellId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.fromCellId = :cellId OR sm.toCellId = :cellId")
    Page<StockMovement> findByCellId(@Param("cellId") Long cellId, Pageable pageable);

    List<StockMovement> findByFromCellId(Long fromCellId);

    List<StockMovement> findByToCellId(Long toCellId);

    // ========== ПОИСК ПО ПЕРИОДУ ==========

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findProductMovementsInPeriod(@Param("productId") Long productId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.warehouseId = :warehouseId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findWarehouseMovementsInPeriod(@Param("warehouseId") Long warehouseId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // ========== АГРЕГАЦИОННЫЕ ЗАПРОСЫ ==========

    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.movementType = :movementType AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    Integer sumMovementsByType(@Param("productId") Long productId,
                               @Param("movementType") MovementType movementType,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm.movementType, SUM(sm.quantity) FROM StockMovement sm WHERE " +
            "sm.warehouseId = :warehouseId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.movementType")
    List<Object[]> summarizeByType(@Param("warehouseId") Long warehouseId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);



        // ========== ПОИСК ПО ССЫЛКАМ ==========

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :refType AND sm.referenceId = :refId")
    List<StockMovement> findByReference(@Param("refType") String refType,
                                        @Param("refId") Long refId);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :refType AND sm.referenceNumber = :refNumber")
    List<StockMovement> findByReferenceNumber(@Param("refType") String refType,
                                              @Param("refNumber") String refNumber);

    /**
     * Находит движения по типу референции, ID референции и ID продукта
     */
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.referenceType = :referenceType AND " +
            "sm.referenceId = :referenceId AND " +
            "sm.productId = :productId")
    List<StockMovement> findByReferenceAndProductId(@Param("referenceType") String referenceType,
                                                    @Param("referenceId") Long referenceId,
                                                    @Param("productId") Long productId);

    // ========== ПОИСК ПО ПАРТИЯМ ==========

    List<StockMovement> findByBatchNumber(String batchNumber);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND sm.productId = :productId")
    List<StockMovement> findByBatchAndProduct(@Param("batchNumber") String batchNumber,
                                              @Param("productId") Long productId);

    // ========== ПОСЛЕДНИЕ ДВИЖЕНИЯ ==========

    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestMovements(Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestWarehouseMovements(@Param("warehouseId") Long warehouseId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND sm.productId = :productId " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findLatestByBatchAndProduct(@Param("batchNumber") String batchNumber,
                                                    @Param("productId") Long productId,
                                                    Pageable pageable);

    /**
     * Найти все движения, связанные с ячейкой (как источник или назначение)
     */
    List<StockMovement> findByFromCellIdOrToCellId(Long fromCellId, Long toCellId);

        // Поиск всех партий продукта
    @Query("SELECT DISTINCT sm.batchNumber FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND sm.batchNumber IS NOT NULL")
    List<String> findBatchNumbersByProduct(@Param("productId") Long productId);

    // Поиск движений по партии за период
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.batchNumber = :batchNumber AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByBatchInPeriod(@Param("batchNumber") String batchNumber,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}