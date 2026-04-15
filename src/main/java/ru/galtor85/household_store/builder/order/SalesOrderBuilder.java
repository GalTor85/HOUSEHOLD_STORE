package ru.galtor85.household_store.builder.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for SalesOrder entities and items.
 */
@Component
@RequiredArgsConstructor
public class SalesOrderBuilder {

    private final NumberGenerator numberGenerator;

    /**
     * Builds a SalesOrder entity from the create request.
     *
     * @param request sales order creation request
     * @param userId  ID of the user placing the order
     * @return SalesOrder entity
     */
    public SalesOrder buildOrder(SalesOrderCreateRequest request, Long userId) {
        return SalesOrder.builder()
                .orderNumber(numberGenerator.generateSalesOrderNumber())
                .userId(userId)
                .orderType(request.getOrderType() != null ? request.getOrderType() : SalesOrderType.RETAIL)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .createdBy(userId)
                .build();
    }

    /**
     * Builds a single SalesOrderItem.
     *
     * @param order   the parent sales order
     * @param itemDto item data from request
     * @param product the product entity
     * @param price   the price for this item
     * @return SalesOrderItem entity
     */
    public SalesOrderItem buildOrderItem(SalesOrder order,
                                         SalesOrderItemCreateDto itemDto,
                                         Product product,
                                         BigDecimal price) {
        return SalesOrderItem.builder()
                .salesOrder(order)
                .productId(product.getId())
                .quantity(itemDto.getQuantity())
                .price(price)
                .productName(product.getName())
                .productSku(product.getSku())
                .build();
    }

    /**
     * Builds a list of SalesOrderItems.
     *
     * @param order    the parent sales order
     * @param itemDtos list of item DTOs
     * @param products list of corresponding products
     * @param prices   list of corresponding prices
     * @return list of SalesOrderItem entities
     */
    public List<SalesOrderItem> buildOrderItems(SalesOrder order,
                                                List<SalesOrderItemCreateDto> itemDtos,
                                                List<Product> products,
                                                List<BigDecimal> prices) {
        List<SalesOrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemDtos.size(); i++) {
            SalesOrderItem item = buildOrderItem(
                    order,
                    itemDtos.get(i),
                    products.get(i),
                    prices.get(i)
            );
            items.add(item);
        }

        return items;
    }

    /**
     * Calculates the total amount for a list of order items.
     *
     * @param items list of sales order items
     * @return total amount
     */
    public BigDecimal calculateTotalAmount(List<SalesOrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}