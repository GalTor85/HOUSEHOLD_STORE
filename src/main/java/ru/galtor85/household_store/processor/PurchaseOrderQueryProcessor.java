package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.PurchaseOrder;
import ru.galtor85.household_store.repository.PurchaseOrderRepository;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderQueryProcessor {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;

    // =========================================================================
    // ОСНОВНЫЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Получает список закупок с фильтрацией
     */
    public Page<PurchaseOrder> getPurchaseOrders(Long supplierId,
                                                 OrderStatus status,
                                                 LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 Pageable pageable) {

        log.debug(messageService.get("purchase.query.processor.search.start",
                supplierId, status, startDate, endDate));

        Page<PurchaseOrder> orders = purchaseOrderRepository.search(
                supplierId, status, startDate, endDate, pageable);

        log.debug(messageService.get("purchase.query.processor.search.complete",
                orders.getTotalElements()));

        return orders;
    }

    // =========================================================================
    // ЗАПРОСЫ ПО СТАТУСАМ
    // =========================================================================

    /**
     * Получает закупки по статусу
     */
    public Page<PurchaseOrder> getPurchaseOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.status", status));
        return purchaseOrderRepository.findByStatus(status, pageable);
    }

    /**
     * Получает закупки по поставщику
     */
    public Page<PurchaseOrder> getPurchaseOrdersBySupplier(Long supplierId, Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.supplier", supplierId));
        return purchaseOrderRepository.findBySupplierId(supplierId, pageable);
    }

    /**
     * Получает закупки по статусу и поставщику
     */
    public Page<PurchaseOrder> getPurchaseOrdersBySupplierAndStatus(Long supplierId,
                                                                    OrderStatus status,
                                                                    Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.supplier.and.status",
                supplierId, status));

        return purchaseOrderRepository.search(supplierId, status, null, null, pageable);
    }

    // =========================================================================
    // ЗАПРОСЫ ПО ДАТАМ
    // =========================================================================

    /**
     * Получает закупки за период
     */
    public Page<PurchaseOrder> getPurchaseOrdersByDateRange(LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.date.range",
                startDate, endDate));

        return purchaseOrderRepository.search(null, null, startDate, endDate, pageable);
    }

    /**
     * Получает закупки по статусу и дате
     */
    public Page<PurchaseOrder> getPurchaseOrdersByStatusAndDateRange(OrderStatus status,
                                                                     LocalDateTime startDate,
                                                                     LocalDateTime endDate,
                                                                     Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.status.and.date",
                status, startDate, endDate));

        return purchaseOrderRepository.search(null, status, startDate, endDate, pageable);
    }

    /**
     * Получает закупки по поставщику и дате
     */
    public Page<PurchaseOrder> getPurchaseOrdersBySupplierAndDateRange(Long supplierId,
                                                                       LocalDateTime startDate,
                                                                       LocalDateTime endDate,
                                                                       Pageable pageable) {
        log.debug(messageService.get("purchase.query.processor.by.supplier.and.date",
                supplierId, startDate, endDate));

        return purchaseOrderRepository.search(supplierId, null, startDate, endDate, pageable);
    }

    // =========================================================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Получает общую сумму закупок у поставщика
     */
    public Double getTotalPurchasesFromSupplier(Long supplierId) {
        Double total = purchaseOrderRepository.getTotalPurchasesFromSupplier(supplierId);
        log.debug(messageService.get("purchase.query.processor.total.purchases",
                supplierId, total != null ? total : 0.0));
        return total != null ? total : 0.0;
    }

    /**
     * Получает количество закупок у поставщика
     */
    public long getPurchaseCountBySupplier(Long supplierId) {
        long count = purchaseOrderRepository.findBySupplierId(supplierId).size();
        log.debug(messageService.get("purchase.query.processor.count.by.supplier",
                supplierId, count));
        return count;
    }

    /**
     * Получает количество закупок по статусу
     */
    public long getPurchaseCountByStatus(OrderStatus status) {
        long count = purchaseOrderRepository.findByStatus(status, Pageable.unpaged()).getTotalElements();
        log.debug(messageService.get("purchase.query.processor.count.by.status",
                status, count));
        return count;
    }

    /**
     * Получает статистику закупок за период
     */
    public PurchaseStatistics getPurchaseStatistics(LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        List<Object[]> stats = purchaseOrderRepository.getPurchaseStats(startDate, endDate);

        long count = 0;
        double totalAmount = 0.0;

        if (stats != null && !stats.isEmpty() && stats.get(0) != null) {
            Object[] row = stats.get(0);
            count = row[0] != null ? ((Number) row[0]).longValue() : 0;
            totalAmount = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        }

        log.debug(messageService.get("purchase.query.processor.statistics",
                startDate, endDate, count, totalAmount));

        return new PurchaseStatistics(count, totalAmount);
    }

    // =========================================================================
    // ПРОВЕРОЧНЫЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Проверяет, есть ли у поставщика незавершенные заказы
     */
    public boolean hasPendingOrders(Long supplierId) {
        boolean hasPending = purchaseOrderRepository.hasPendingOrders(supplierId);
        log.debug(messageService.get("purchase.query.processor.has.pending.orders",
                supplierId, hasPending));
        return hasPending;
    }

    /**
     * Проверяет, есть ли у поставщика незавершенные закупки
     */
    public boolean hasPendingPurchases(Long supplierId) {
        boolean hasPending = purchaseOrderRepository.hasPendingPurchases(supplierId);
        log.debug(messageService.get("purchase.query.processor.has.pending.purchases",
                supplierId, hasPending));
        return hasPending;
    }

    // =========================================================================
    // ВНУТРЕННИЙ КЛАСС ДЛЯ СТАТИСТИКИ
    // =========================================================================

    @lombok.Value
    public static class PurchaseStatistics {
        long totalOrders;
        double totalAmount;
    }
}