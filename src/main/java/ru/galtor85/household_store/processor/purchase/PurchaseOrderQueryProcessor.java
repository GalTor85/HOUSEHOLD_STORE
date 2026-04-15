package ru.galtor85.household_store.processor.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.time.LocalDateTime;

/**
 * Processor for purchase order queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderQueryProcessor {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final LogMessageService logMsg;

    // =========================================================================
    // MAIN QUERIES
    // =========================================================================

    /**
     * Retrieves paginated purchase orders with filters.
     *
     * @param supplierId supplier ID filter (optional)
     * @param status     order status filter (optional)
     * @param startDate  start date filter (optional)
     * @param endDate    end date filter (optional)
     * @param pageable   pagination information
     * @return page of PurchaseOrder entities
     */
    public Page<PurchaseOrder> getPurchaseOrders(Long supplierId,
                                                 OrderStatus status,
                                                 LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 Pageable pageable) {

        log.debug(logMsg.get("purchase.query.processor.search.start",
                supplierId, status, startDate, endDate));

        Page<PurchaseOrder> orders = purchaseOrderRepository.search(
                supplierId, status, startDate, endDate, pageable);

        log.debug(logMsg.get("purchase.query.processor.search.complete",
                orders.getTotalElements()));

        return orders;
    }
}