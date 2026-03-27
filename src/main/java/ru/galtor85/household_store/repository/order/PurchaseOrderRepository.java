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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // =========================================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // =========================================================================

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    Page<PurchaseOrder> findByStatus(OrderStatus status, Pageable pageable);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    List<PurchaseOrder> findByReceivedBy(Long receivedBy);

    Optional<PurchaseOrder> findByInvoiceNumber(String invoiceNumber);

    // =========================================================================
    // ЗАПРОСЫ ПО СТАТУСАМ
    // =========================================================================

    @Query("SELECT po FROM PurchaseOrder po WHERE po.paymentStatus = :paymentStatus")
    List<PurchaseOrder> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.qualityCheck = :qualityCheck")
    List<PurchaseOrder> findByQualityCheck(@Param("qualityCheck") Boolean qualityCheck);

    // =========================================================================
    // ЗАПРОСЫ ПО ДАТАМ
    // =========================================================================

    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByExpectedDeliveryBetween(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery = :today")
    List<PurchaseOrder> findDueToday(@Param("today") LocalDate today);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery < :today AND po.actualDelivery IS NULL")
    List<PurchaseOrder> findOverdueDeliveries(@Param("today") LocalDate today);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.paymentDue < :currentDate AND po.paymentStatus != 'PAID'")
    List<PurchaseOrder> findOverduePayments(@Param("currentDate") LocalDate currentDate);

    // =========================================================================
    // ПРОВЕРКИ
    // =========================================================================

    /**
     * Проверяет, есть ли у поставщика незавершенные заказы
     * Незавершенными считаются заказы в статусах:
     * PENDING, PAID, PROCESSING, SHIPPED, PARTIALLY_RECEIVED
     */
    @Query("SELECT COUNT(po) > 0 FROM PurchaseOrder po " +
            "WHERE po.supplierId = :supplierId " +
            "AND po.status IN ('PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'PARTIALLY_RECEIVED')")
    boolean hasPendingOrders(@Param("supplierId") Long supplierId);

    /**
     * Проверяет, есть ли у поставщика незавершенные закупки (по фактической поставке)
     */
    @Query("SELECT COUNT(po) > 0 FROM PurchaseOrder po " +
            "WHERE po.supplierId = :supplierId AND po.actualDelivery IS NULL")
    boolean hasPendingPurchases(@Param("supplierId") Long supplierId);

    // =========================================================================
    // СТАТИСТИКА
    // =========================================================================

    /**
     * Получить сумму всех закупок у поставщика
     */
    @Query("SELECT SUM(po.totalAmount) FROM PurchaseOrder po WHERE po.supplierId = :supplierId")
    Double getTotalPurchasesFromSupplier(@Param("supplierId") Long supplierId);

    /**
     * Получить статистику закупок за период
     */
    @Query("SELECT COUNT(po), SUM(po.totalAmount) FROM PurchaseOrder po " +
            "WHERE po.createdAt BETWEEN :startDate AND :endDate")
    List<Object[]> getPurchaseStats(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Получить общее количество принятых товаров по заказу
     */
    @Query("SELECT SUM(oi.receivedQuantity) FROM PurchaseOrderItem oi " +
            "WHERE oi.purchaseOrder.id = :orderId")
    Integer getTotalReceivedQuantity(@Param("orderId") Long orderId);

    // =========================================================================
    // ПОИСК С ФИЛЬТРАЦИЕЙ
    // =========================================================================

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

    // =========================================================================
    // ОБНОВЛЕНИЕ И УДАЛЕНИЕ
    // =========================================================================

    /**
     * Обновить статус оплаты
     */
    @Modifying
    @Query("UPDATE PurchaseOrder po SET po.paymentStatus = :paymentStatus WHERE po.id = :purchaseOrderId")
    int updatePaymentStatus(@Param("purchaseOrderId") Long purchaseOrderId,
                            @Param("paymentStatus") String paymentStatus);

    /**
     * Обновить статус заказа
     */
    @Modifying
    @Query("UPDATE PurchaseOrder po SET po.status = :status WHERE po.id = :orderId")
    int updateOrderStatus(@Param("orderId") Long orderId,
                          @Param("status") OrderStatus status);
}