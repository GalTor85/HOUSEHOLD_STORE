package ru.galtor85.household_store.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrder.ReservationStatus;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.warehouse.WarehouseSelectionService;

import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_RESERVATION_DAYS;

/**
 * Service for managing product reservation for cash payments.
 *
 * @author G@LTor85
 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final ProductStockRepository productStockRepository;
    private final WarehouseSelectionService warehouseSelectionService;

    /**
     * Reserves products for an order.
     *
     * @param order the sales order
     * @return updated order with reservation info
     */
    @Transactional
    public SalesOrder reserveOrder(SalesOrder order) {
        log.info(logMsg.get("reservation.start", order.getId()));

        // Check if already reserved
        if (order.getReservationStatus() == ReservationStatus.ACTIVE) {
            log.debug(logMsg.get("reservation.already.active", order.getId()));
            return order;
        }

        // Get default warehouse for reservation (or determine from order)
        Long warehouseId = warehouseSelectionService.selectWarehouseForReservation(order);

        // Reserve products
        for (SalesOrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageService.get("product.not.found", item.getProductId())));

            // Get available stock at warehouse
            ProductStock stock = productStockRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                    .orElse(null);

            int availableStock = stock != null ? stock.getAvailableQuantity() : 0;

            // Check stock availability
            if (availableStock < item.getQuantity()) {
                throw new IllegalStateException(
                        messageService.get("reservation.insufficient.stock",
                                product.getName(), availableStock, item.getQuantity()));
            }

            // Reserve stock (increase reservedQuantity, decrease availableQuantity)
            if (stock == null) {
                // Create new stock record with reserved quantity
                stock = ProductStock.builder()
                        .productId(product.getId())
                        .warehouseId(warehouseId)
                        .quantity(0)
                        .reservedQuantity(item.getQuantity())
                        .availableQuantity(-item.getQuantity()) // Negative until stock arrives
                        .createdAt(LocalDateTime.now())
                        .build();
            } else {
                stock.setReservedQuantity(stock.getReservedQuantity() + item.getQuantity());
                stock.setAvailableQuantity(stock.getQuantity() - stock.getReservedQuantity());
                stock.setUpdatedAt(LocalDateTime.now());
            }
            productStockRepository.save(stock);

            log.debug(logMsg.get("reservation.product.reserved",
                    product.getSku(), warehouseId, item.getQuantity(),
                    stock.getReservedQuantity(), stock.getAvailableQuantity()));
        }

        // Set reservation info
        order.setReservationStatus(ReservationStatus.ACTIVE);
        order.setReservedUntil(LocalDateTime.now().plusDays(DEFAULT_RESERVATION_DAYS));

        SalesOrder savedOrder = salesOrderRepository.save(order);

        log.info(logMsg.get("reservation.complete", order.getId(), DEFAULT_RESERVATION_DAYS));

        return savedOrder;
    }


    /**
     * Releases reserved products (when reservation expires or is cancelled).
     *
     * @param order the sales order
     */
    @Transactional
    public void releaseReservation(SalesOrder order) {
        if (order.getReservationStatus() != ReservationStatus.ACTIVE) {
            log.debug(logMsg.get("reservation.not.active", order.getId()));
            return;
        }

        log.info(logMsg.get("reservation.release.start", order.getId()));

        Long warehouseId = warehouseSelectionService.selectWarehouseForReservation(order);

        // Release reserved products (decrease reservedQuantity, increase availableQuantity)
        for (SalesOrderItem item : order.getItems()) {
            ProductStock stock = productStockRepository
                    .findByProductIdAndWarehouseId(item.getProductId(), warehouseId)
                    .orElse(null);

            if (stock != null) {
                int oldReserved = stock.getReservedQuantity();
                int newReserved = oldReserved - item.getQuantity();

                stock.setReservedQuantity(newReserved);
                stock.setAvailableQuantity(stock.getQuantity() - newReserved);
                stock.setUpdatedAt(LocalDateTime.now());
                productStockRepository.save(stock);

                log.debug(logMsg.get("reservation.product.released",
                        item.getProductId(), warehouseId,
                        oldReserved, newReserved, stock.getAvailableQuantity()));
            }
        }

        // Update reservation status
        order.setReservationStatus(ReservationStatus.EXPIRED);
        salesOrderRepository.save(order);

        log.info(logMsg.get("reservation.release.complete", order.getId()));
    }
    /**
     * Completes reservation (when order is paid).
     *
     * @param order the sales order
     */
    @Transactional
    public void completeReservation(SalesOrder order) {
        if (order.getReservationStatus() != ReservationStatus.ACTIVE) {
            log.debug(logMsg.get("reservation.not.active", order.getId()));
            return;
        }

        log.info(logMsg.get("reservation.complete.status", order.getId()));

        order.setReservationStatus(ReservationStatus.COMPLETED);
        salesOrderRepository.save(order);

        log.info(logMsg.get("reservation.complete.status.done", order.getId()));
    }
}