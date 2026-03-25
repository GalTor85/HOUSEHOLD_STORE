package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    // =========================================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Находит заказ по номеру
     */
    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    /**
     * Находит все заказы пользователя
     */
    List<SalesOrder> findByUserId(Long userId);

    /**
     * Находит заказы пользователя с пагинацией
     */
    Page<SalesOrder> findByUserId(Long userId, Pageable pageable);

    /**
     * Находит заказы по статусу
     */
    Page<SalesOrder> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Находит заказы по трек-номеру
     */
    Optional<SalesOrder> findByTrackingNumber(String trackingNumber);

    // =========================================================================
    // ЗАПРОСЫ С ФИЛЬТРАЦИЕЙ ПО ДАТАМ
    // =========================================================================

    /**
     * Находит заказы пользователя за период
     */
    @Query("SELECT so FROM SalesOrder so WHERE so.userId = :userId " +
            "AND so.createdAt BETWEEN :startDate AND :endDate")
    List<SalesOrder> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Находит заказы пользователя по статусу за период
     */
    @Query("SELECT so FROM SalesOrder so WHERE so.userId = :userId " +
            "AND so.status = :status " +
            "AND so.createdAt BETWEEN :startDate AND :endDate")
    Page<SalesOrder> findByUserIdAndStatusAndCreatedAtBetween(@Param("userId") Long userId,
                                                              @Param("status") OrderStatus status,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate,
                                                              Pageable pageable);

    // =========================================================================
    // РАСШИРЕННЫЙ ПОИСК
    // =========================================================================

    /**
     * Поиск заказов с фильтрацией
     */
    @Query("SELECT so FROM SalesOrder so WHERE " +
            "(:userId IS NULL OR so.userId = :userId) AND " +
            "(:status IS NULL OR so.status = :status) AND " +
            "(:startDate IS NULL OR so.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR so.createdAt <= :endDate)")
    Page<SalesOrder> search(@Param("userId") Long userId,
                            @Param("status") OrderStatus status,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            Pageable pageable);

    /**
     * Поиск заказов по тексту (номер заказа, трек-номер)
     */
    @Query("SELECT so FROM SalesOrder so WHERE " +
            "LOWER(so.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(so.trackingNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<SalesOrder> searchByTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    // =========================================================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Подсчитывает количество заказов за период
     */
    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Подсчитывает количество заказов пользователя за период
     */
    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.userId = :userId " +
            "AND so.createdAt BETWEEN :startDate AND :endDate")
    long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Суммирует общую сумму заказов за период
     */
    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so " +
            "WHERE so.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Суммирует общую сумму заказов пользователя за период
     */
    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so " +
            "WHERE so.userId = :userId AND so.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Получает общую сумму всех заказов пользователя
     */
    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so WHERE so.userId = :userId")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);

    // =========================================================================
    // АНАЛИТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Получает топ продаваемых товаров
     */
    @Query("SELECT soi.productId, SUM(soi.quantity) as totalSold " +
            "FROM SalesOrderItem soi " +
            "JOIN soi.salesOrder so " +
            "WHERE so.status = 'COMPLETED' " +
            "GROUP BY soi.productId " +
            "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);

    /**
     * Получает статистику заказов по дням за период
     */
    @Query("SELECT DATE(so.createdAt), COUNT(so), SUM(so.totalAmount) " +
            "FROM SalesOrder so " +
            "WHERE so.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(so.createdAt) " +
            "ORDER BY DATE(so.createdAt)")
    List<Object[]> getDailyStats(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Получает статистику по статусам заказов
     */
    @Query("SELECT so.status, COUNT(so), SUM(so.totalAmount) " +
            "FROM SalesOrder so " +
            "GROUP BY so.status")
    List<Object[]> getStatsByStatus();

    // =========================================================================
    // ОБНОВЛЕНИЯ
    // =========================================================================

    /**
     * Обновляет статус заказа
     */
    @Modifying
    @Query("UPDATE SalesOrder so SET so.status = :status WHERE so.id = :orderId")
    int updateOrderStatus(@Param("orderId") Long orderId,
                          @Param("status") OrderStatus status);

    /**
     * Обновляет трек-номер заказа
     */
    @Modifying
    @Query("UPDATE SalesOrder so SET so.trackingNumber = :trackingNumber WHERE so.id = :orderId")
    int updateTrackingNumber(@Param("orderId") Long orderId,
                             @Param("trackingNumber") String trackingNumber);
}