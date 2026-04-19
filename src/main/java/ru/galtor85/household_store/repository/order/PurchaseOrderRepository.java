package ru.galtor85.household_store.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Finds a purchase order by its order number.
     *
     * @param orderNumber order number
     * @return purchase order entity or empty if not found
     */
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    /**
     * Searches for purchase orders based on optional filters.
     *
     * @param supplierId supplier ID
     * @param status     order status
     * @param startDate  start date
     * @param endDate    end date
     * @param pageable   pagination and sorting options
     * @return page of purchase orders
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE " +
            "(:supplierId IS NULL OR po.supplierId = :supplierId) AND " +
            "(:status IS NULL OR po.status = :status) AND " +
            "(CAST(:startDate AS timestamp) IS NULL OR po.createdAt >= :startDate) AND " +
            "(CAST(:endDate AS timestamp) IS NULL OR po.createdAt <= :endDate)")
    Page<PurchaseOrder> search(@Param("supplierId") Long supplierId,
                               @Param("status") OrderStatus status,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate,
                               Pageable pageable);

    /**
     * Sums total amount of all purchase orders by supplier.
     *
     * @param supplierId supplier ID
     * @return total amount
     */
    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE po.supplierId = :supplierId")
    BigDecimal sumTotalAmountBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * Finds maximum created date of purchase orders by supplier.
     */
    @Query("SELECT MAX(po.createdAt) FROM PurchaseOrder po WHERE po.supplierId = :supplierId")
    LocalDateTime findMaxCreatedAtBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT COUNT(po) > 0 FROM PurchaseOrder po " +
            "WHERE po.supplierId = :supplierId " +
            "AND po.status NOT IN ('CANCELLED', 'COMPLETED')")
    boolean existsActiveBySupplierId(@Param("supplierId") Long supplierId);

    @Modifying
    @Query("DELETE FROM PurchaseOrder o WHERE o.deleted = true AND o.deletedAt < :threshold")
    int deleteByDeletedTrueAndDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}