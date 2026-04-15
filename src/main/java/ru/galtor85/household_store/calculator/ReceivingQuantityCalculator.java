package ru.galtor85.household_store.calculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Calculator for receiving quantities during purchase order processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceivingQuantityCalculator {

    private final ProductRepository productRepository;
    private final LogMessageService logMsg;

    /**
     * Calculates the actual quantity to receive for an order item.
     *
     * @param orderItem the purchase order item
     * @param item      the received item request
     * @return ReceivingResult containing product and calculated quantity
     */
    public ReceivingResult calculate( PurchaseOrderItem orderItem, ReceiveStockItem item) {
        Product product = productRepository.findById(orderItem.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(orderItem.getProductId()));

        int alreadyReceived = orderItem.getReceivedQuantity() != null ? orderItem.getReceivedQuantity() : 0;
        int orderedQuantity = orderItem.getQuantity();
        int remainingToReceive = orderedQuantity - alreadyReceived;

        int receivingQuantity = item.getQuantity();

        if (receivingQuantity > remainingToReceive) {
            log.warn(logMsg.get("receiving.quantity.exceeds.remaining",
                    product.getSku(), receivingQuantity, remainingToReceive));
            receivingQuantity = remainingToReceive;
        }

        if (receivingQuantity <= 0) {
            log.info(logMsg.get("receiving.already.fully.received", product.getSku()));
        }

        return new ReceivingResult(product, receivingQuantity, alreadyReceived);
    }

    /**
     * Result of receiving quantity calculation.
     */
    public record ReceivingResult(Product product, int receivingQuantity, int alreadyReceived) {

        public boolean hasNoQuantity() {
            return receivingQuantity <= 0;
        }
    }
}