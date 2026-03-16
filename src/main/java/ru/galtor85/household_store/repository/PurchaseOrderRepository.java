package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.PurchaseOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Найти закупку по ID заказа
     */
    Optional<PurchaseOrder> findByOrderId(Long orderId);

    /**
     * Найти все закупки по ID поставщика
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.order.supplierId = :supplierId")
    List<PurchaseOrder> findBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * Найти все закупки по ID поставщика с пагинацией
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.order.supplierId = :supplierId")
    Page<PurchaseOrder> findBySupplierId(@Param("supplierId") Long supplierId, Pageable pageable);

    /**
     * Найти закупки по статусу оплаты
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.paymentStatus = :paymentStatus")
    List<PurchaseOrder> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    /**
     * Найти закупки с просроченной оплатой
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.paymentDue < :currentDate AND po.paymentStatus != 'PAID'")
    List<PurchaseOrder> findOverduePayments(@Param("currentDate") LocalDate currentDate);

    /**
     * Найти закупки с ожидаемой поставкой в диапазоне дат
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByExpectedDeliveryBetween(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Найти закупки, которые должны поступить сегодня
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery = :today")
    List<PurchaseOrder> findDueToday(@Param("today") LocalDate today);

    /**
     * Найти закупки с просроченной поставкой
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery < :today AND po.actualDelivery IS NULL")
    List<PurchaseOrder> findOverdueDeliveries(@Param("today") LocalDate today);

    /**
     * Найти закупки, прошедшие проверку качества
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.qualityCheck = :qualityCheck")
    List<PurchaseOrder> findByQualityCheck(@Param("qualityCheck") Boolean qualityCheck);

    /**
     * Найти закупки по номеру счета-фактуры
     */
    Optional<PurchaseOrder> findByInvoiceNumber(String invoiceNumber);

    /**
     * Найти закупки, принятые конкретным сотрудником
     */
    List<PurchaseOrder> findByReceivedBy(Long receivedBy);

    /**
     * Получить статистику закупок за период
     */
    @Query("SELECT COUNT(po), SUM(po.order.totalAmount) FROM PurchaseOrder po " +
            "WHERE po.order.createdAt BETWEEN :startDate AND :endDate")
    List<Object[]> getPurchaseStats(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Получить сумму всех закупок у поставщика
     */
    @Query("SELECT SUM(po.order.totalAmount) FROM PurchaseOrder po WHERE po.order.supplierId = :supplierId")
    Double getTotalPurchasesFromSupplier(@Param("supplierId") Long supplierId);

    /**
     * Проверить, есть ли незавершенные закупки у поставщика
     */
    @Query("SELECT COUNT(po) > 0 FROM PurchaseOrder po " +
            "WHERE po.order.supplierId = :supplierId AND po.actualDelivery IS NULL")
    boolean hasPendingPurchases(@Param("supplierId") Long supplierId);

    /**
     * Обновить статус оплаты
     */
    @Modifying
    @Query("UPDATE PurchaseOrder po SET po.paymentStatus = :paymentStatus WHERE po.id = :purchaseOrderId")
    int updatePaymentStatus(@Param("purchaseOrderId") Long purchaseOrderId,
                            @Param("paymentStatus") String paymentStatus);

    /**
     * Удалить все закупки по ID заказа
     */
    @Modifying
    @Query("DELETE FROM PurchaseOrder po WHERE po.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}