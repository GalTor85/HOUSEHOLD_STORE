package ru.galtor85.household_store.util.calculator;

import lombok.experimental.UtilityClass;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptItem;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Utility class for refund amount calculations.
 */
@UtilityClass
public class RefundCalculator {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculates total refund amount based on returned items.
     *
     * @param order purchase order
     * @param itemsToReverse items to reverse (null or empty = reverse all)
     * @return total refund amount
     */
    public static BigDecimal calculateTotalRefundAmount(PurchaseOrder order,
                                                        List<ReverseReceiptItem> itemsToReverse) {
        boolean reverseAll = (itemsToReverse == null || itemsToReverse.isEmpty());
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (PurchaseOrderItem orderItem : order.getItems()) {
            int receivedQuantity = orderItem.getReceivedQuantity() != null ?
                    orderItem.getReceivedQuantity() : 0;

            if (receivedQuantity == 0) {
                continue;
            }

            int quantityToReverse = calculateQuantityToReverse(orderItem, itemsToReverse, reverseAll);

            if (quantityToReverse > 0) {
                BigDecimal itemRefund = orderItem.getPrice()
                        .multiply(BigDecimal.valueOf(quantityToReverse));
                totalRefund = totalRefund.add(itemRefund);
            }
        }

        return totalRefund;
    }

    /**
     * Calculates proportional refund amount for a specific payment.
     *
     * @param paymentAmount amount of the payment
     * @param invoiceAmount total invoice amount
     * @param totalRefund total refund amount for the order
     * @return proportional refund amount for this payment
     */
    public static BigDecimal calculateProportionalRefund(BigDecimal paymentAmount,
                                                         BigDecimal invoiceAmount,
                                                         BigDecimal totalRefund) {
        if (invoiceAmount == null || invoiceAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (totalRefund == null || totalRefund.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate what portion of the invoice this payment represents
        BigDecimal paymentRatio = paymentAmount.divide(invoiceAmount, SCALE, ROUNDING_MODE);

        // Calculate proportional refund
        return totalRefund.multiply(paymentRatio);
    }

    /**
     * Calculates quantity to reverse for an order item.
     *
     * @param orderItem purchase order item
     * @param itemsToReverse list of items to reverse
     * @param reverseAll true if reversing all items
     * @return quantity to reverse
     */
    private static int calculateQuantityToReverse(PurchaseOrderItem orderItem,
                                                  List<ReverseReceiptItem> itemsToReverse,
                                                  boolean reverseAll) {
        int receivedQuantity = orderItem.getReceivedQuantity() != null ?
                orderItem.getReceivedQuantity() : 0;

        if (reverseAll) {
            return receivedQuantity;
        }

        return itemsToReverse.stream()
                .filter(item -> item.getProductId().equals(orderItem.getProductId()))
                .findFirst()
                .map(item -> Math.min(item.getQuantity(), receivedQuantity))
                .orElse(0);
    }
}