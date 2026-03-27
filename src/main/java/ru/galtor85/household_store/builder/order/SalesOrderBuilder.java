package ru.galtor85.household_store.builder.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SalesOrderBuilder {

    private final NumberGenerator numberGenerator;

    /**
     * Создает сущность заказа на продажу из запроса
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
     * Создает позицию заказа на продажу
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
     * Создает список позиций заказа
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
     * Рассчитывает общую сумму заказа
     */
    public BigDecimal calculateTotalAmount(List<SalesOrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}