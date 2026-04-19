package ru.galtor85.household_store.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SalesOrder entity.
 * Provides methods for searching, filtering, and aggregating sales orders.
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    /**
     * Finds a sales order by its unique order number.
     *
     * @param orderNumber the unique order number
     * @return optional sales order
     */
    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    /**
     * Finds all sales orders for a specific user.
     *
     * @param userId the user identifier
     * @return list of sales orders
     */
    List<SalesOrder> findByUserId(Long userId);

    /**
     * Searches sales orders with optional filters.
     * All parameters are optional - if null, the filter is ignored.
     *
     * @param userId    user identifier filter (optional)
     * @param status    order status filter (optional)
     * @param startDate start date filter (optional, inclusive)
     * @param endDate   end date filter (optional, inclusive)
     * @param pageable  pagination information
     * @return page of matching sales orders
     */
    @Query("SELECT so FROM SalesOrder so WHERE " +
            "(COALESCE(:userId, so.userId) = so.userId) AND " +
            "(COALESCE(:status, so.status) = so.status) AND " +
            "(COALESCE(:startDate, so.createdAt) <= so.createdAt) AND " +
            "(COALESCE(:endDate, so.createdAt) >= so.createdAt)")
    Page<SalesOrder> search(@Param("userId") Long userId,
                            @Param("status") OrderStatus status,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            Pageable pageable);

    /**
     * Counts the number of sales orders for a specific user.
     *
     * @param userId the user identifier
     * @return total count of orders for the user
     */
    long countByUserId(Long userId);

    /**
     * Gets daily sales report for a specific date.
     */
    @Query("SELECT COUNT(so), " +
            "SUM(CASE WHEN so.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN so.status = 'CANCELLED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN so.status = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(so.totalAmount), " +
            "MIN(so.totalAmount), " +
            "MAX(so.totalAmount), " +
            "COUNT(DISTINCT so.userId) " +
            "FROM SalesOrder so " +
            "WHERE DATE(so.createdAt) = :date")
    Object[] getDailyReport(@Param("date") LocalDate date);

    /**
     * Gets top products for daily report.
     */
    @Query("SELECT soi.productId, soi.productName, " +
            "SUM(soi.quantity), SUM(soi.price * soi.quantity) " +
            "FROM SalesOrderItem soi " +
            "JOIN SalesOrder so ON soi.salesOrder.id = so.id " +
            "WHERE DATE(so.createdAt) = :date " +
            "GROUP BY soi.productId, soi.productName " +
            "ORDER BY SUM(soi.quantity) DESC")
    List<Object[]> getTopProductsForDate(@Param("date") LocalDate date);

    /**
     * Gets hourly sales for daily report.
     */
    @Query("SELECT EXTRACT(HOUR FROM so.createdAt), " +
            "COUNT(so), SUM(so.totalAmount) " +
            "FROM SalesOrder so " +
            "WHERE DATE(so.createdAt) = :date " +
            "GROUP BY EXTRACT(HOUR FROM so.createdAt) " +
            "ORDER BY EXTRACT(HOUR FROM so.createdAt)")
    List<Object[]> getHourlySalesForDate(@Param("date") LocalDate date);

    /**
     *
     * @param id
     * @param deletedAt
     * @param deletedBy
     * @param reason
     * @return
     */
    @Modifying
    @Query("UPDATE SalesOrder o SET o.deleted = true, o.deletedAt = :deletedAt, o.deletedBy = :deletedBy, o.deleteReason = :reason WHERE o.id = :id")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt,
                   @Param("deletedBy") Long deletedBy, @Param("reason") String reason);

    /**
     *
     * @param threshold
     * @return
     */
    @Modifying
    @Query("DELETE FROM SalesOrder o WHERE o.deleted = true AND o.deletedAt < :threshold")
    int deleteByDeletedTrueAndDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
