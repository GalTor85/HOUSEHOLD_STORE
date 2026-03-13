package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Найти все позиции заказа
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Найти все позиции заказа с пагинацией
     */
    Page<OrderItem> findByOrderId(Long orderId, Pageable pageable);

    /**
     * Найти все позиции по товару
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Найти все позиции по товару поставщика
     */
    List<OrderItem> findBySupplierProductId(Long supplierProductId);

    /**
     * Найти позиции по товару за период
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId " +
            "AND oi.order.createdAt BETWEEN :startDate AND :endDate")
    List<OrderItem> findByProductIdAndDateRange(@Param("productId") Long productId,
                                                @Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Получить статистику продаж товара
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId " +
            "AND oi.order.status = 'COMPLETED'")
    Long getTotalSoldQuantity(@Param("productId") Long productId);

    /**
     * Получить общую выручку по товару
     */
    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.productId = :productId " +
            "AND oi.order.status = 'COMPLETED'")
    BigDecimal getTotalRevenueByProduct(@Param("productId") Long productId);

    /**
     * Получить топ продаваемых товаров
     */
    @Query("SELECT oi.productId, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi WHERE oi.order.status = 'COMPLETED' " +
            "GROUP BY oi.productId ORDER BY totalSold DESC")
    List<Object[]> getTopSellingProducts(Pageable pageable);

    /**
     * Получить статистику по поставщику
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.supplierProductId IN " +
            "(SELECT sp.id FROM SupplierProduct sp WHERE sp.supplierId = :supplierId)")
    List<OrderItem> findBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * Удалить все позиции заказа
     */
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * Проверить, есть ли товар в заказах
     */
    boolean existsByProductId(Long productId);

    /**
     * Получить среднюю цену продажи товара
     */
    @Query("SELECT AVG(oi.price) FROM OrderItem oi WHERE oi.productId = :productId " +
            "AND oi.order.status = 'COMPLETED'")
    BigDecimal getAverageSalePrice(@Param("productId") Long productId);

    /**
     * Получить количество возвратов товара
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.productId = :productId " +
            "AND oi.order.status = 'REFUNDED'")
    long countRefundsByProduct(@Param("productId") Long productId);

    /**
     * Получить дневную статистику продаж
     */
    @Query("SELECT DATE(oi.order.createdAt), SUM(oi.quantity), SUM(oi.totalPrice) " +
            "FROM OrderItem oi WHERE oi.productId = :productId " +
            "GROUP BY DATE(oi.order.createdAt) ORDER BY DATE(oi.order.createdAt)")
    List<Object[]> getDailySalesStats(@Param("productId") Long productId);

    /**
     * Получить все позиции заказа с деталями товаров (оптимизированный запрос)
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    /**
     * Получить все позиции по нескольким заказам
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}