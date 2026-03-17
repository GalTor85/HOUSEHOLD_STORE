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

    List<StockMovement> findByProductId(Long productId);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    List<StockMovement> findByFromCellIdOrToCellId(Long fromCellId, Long toCellId);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findProductMovementsInPeriod(@Param("productId") Long productId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE " +
            "sm.productId = :productId AND " +
            "sm.movementType = :movementType AND " +
            "sm.createdAt BETWEEN :startDate AND :endDate")
    Integer sumMovementsByType(@Param("productId") Long productId,
                               @Param("movementType") MovementType movementType,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
}