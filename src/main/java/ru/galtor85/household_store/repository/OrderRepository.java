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

    @Query("SELECT o FROM Order o WHERE " +
            "(:userId IS NULL OR o.userId = :userId) AND " +
            "(:supplierId IS NULL OR o.supplierId = :supplierId) AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:orderType IS NULL OR o.orderType = :orderType) AND " +
            "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR o.createdAt <= :endDate)")
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
}