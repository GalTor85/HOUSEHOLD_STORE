package ru.galtor85.household_store.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrder.ReservationStatus;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_RESERVATION_DAYS;

/**
 * Service for managing product reservation for cash payments.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

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

        // Reserve products
        for (SalesOrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageService.get("product.not.found", item.getProductId())));

            // Check stock availability
            if (product.getQuantityInStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        messageService.get("reservation.insufficient.stock",
                                product.getName(), product.getQuantityInStock(), item.getQuantity()));
            }

            // Reduce stock (reserve)
            product.setQuantityInStock(product.getQuantityInStock() - item.getQuantity());
            productRepository.save(product);
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

        // Return products to stock
        for (SalesOrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setQuantityInStock(product.getQuantityInStock() + item.getQuantity());
                productRepository.save(product);
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