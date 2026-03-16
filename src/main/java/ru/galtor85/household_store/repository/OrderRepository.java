package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findBySupplierId(Long supplierId);

    Page<Order> findBySupplierId(Long supplierId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.orderType = :orderType")
    List<Order> findByOrderType(@Param("orderType") OrderType orderType);

    // ИСПРАВЛЕННЫЙ JPQL запрос с использованием CAST
    @Query("SELECT o FROM Order o WHERE " +
            "(cast(:userId as long) IS NULL OR o.userId = :userId) AND " +
            "(cast(:supplierId as long) IS NULL OR o.supplierId = :supplierId) AND " +
            "(cast(:status as string) IS NULL OR o.status = :status) AND " +
            "(cast(:orderType as string) IS NULL OR o.orderType = :orderType) AND " +
            "(cast(:startDate as date) IS NULL OR o.createdAt >= :startDate) AND " +
            "(cast(:endDate as date) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> searchOrders(@Param("userId") Long userId,
                             @Param("supplierId") Long supplierId,
                             @Param("status") OrderStatus status,
                             @Param("orderType") OrderType orderType,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status = 'COMPLETED'")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.supplierId = :supplierId")
    long countBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED'")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    Page<Order> findByUserIdAndStatusAndCreatedAtBetween(Long customerId, OrderStatus orderStatus, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByUserIdAndStatusAndCreatedAtAfter(Long customerId, OrderStatus orderStatus, LocalDateTime start, Pageable pageable);

    Page<Order> findByUserIdAndStatusAndCreatedAtBefore(Long customerId, OrderStatus orderStatus, LocalDateTime end, Pageable pageable);

    Page<Order> findByUserIdAndCreatedAtBetween(Long customerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByStatusAndCreatedAtBetween(OrderStatus orderStatus, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long customerId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByUserIdAndCreatedAtAfter(Long customerId, LocalDateTime start, Pageable pageable);

    Page<Order> findByUserIdAndCreatedAtBefore(Long customerId, LocalDateTime end, Pageable pageable);

    Page<Order> findByStatusAndCreatedAtAfter(OrderStatus orderStatus, LocalDateTime start, Pageable pageable);

    Page<Order> findByStatusAndCreatedAtBefore(OrderStatus orderStatus, LocalDateTime end, Pageable pageable);

    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByCreatedAtAfter(LocalDateTime start, Pageable pageable);

    Page<Order> findByCreatedAtBefore(LocalDateTime end, Pageable pageable);

    Page<Order> findByStatus(OrderStatus orderStatus, Pageable pageable);

}