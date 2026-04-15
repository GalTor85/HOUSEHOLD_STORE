package ru.galtor85.household_store.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SalesOrder entity.
 * Provides methods for searching, filtering, and aggregating sales orders.
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    // =========================================================================
    // BASIC QUERIES
    // =========================================================================

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

    // =========================================================================
    // ADVANCED SEARCH
    // =========================================================================

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

    // =========================================================================
    // STATISTICAL QUERIES
    // =========================================================================

    /**
     * Counts the number of sales orders created within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return total count of orders in the period
     */
    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Sums the total amount of sales orders created within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return total amount of orders in the period, or 0 if none
     */
    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so " +
            "WHERE so.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Counts the number of sales orders for a specific user.
     *
     * @param userId the user identifier
     * @return total count of orders for the user
     */
    long countByUserId(Long userId);
}
