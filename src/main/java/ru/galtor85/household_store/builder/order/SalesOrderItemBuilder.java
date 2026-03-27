package ru.galtor85.household_store.builder.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderItemBuilder {

    /**
     * Создает позицию заказа из DTO
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
     * Создает позицию заказа из параметров
     */
    public SalesOrderItem buildOrderItem(SalesOrder order,
                                         Long productId,
                                         Integer quantity,
                                         BigDecimal price,
                                         Product product) {

        return SalesOrderItem.builder()
                .salesOrder(order)
                .productId(productId)
                .quantity(quantity)
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

            log.debug("Created order item: productId={}, quantity={}, price={}",
                    item.getProductId(), item.getQuantity(), item.getPrice());
        }

        return items;
    }

    /**
     * Создает копию позиции (для возврата или отмены)
     */
    public SalesOrderItem buildCopy(SalesOrderItem original, SalesOrder newOrder) {
        return SalesOrderItem.builder()
                .salesOrder(newOrder)
                .productId(original.getProductId())
                .quantity(original.getQuantity())
                .price(original.getPrice())
                .productName(original.getProductName())
                .productSku(original.getProductSku())
                .build();
    }

    /**
     * Создает позицию возврата (с отрицательным количеством)
     */
    public SalesOrderItem buildReturnItem(SalesOrder order,
                                          Product product,
                                          Integer quantity,
                                          BigDecimal price,
                                          String reason) {

        return SalesOrderItem.builder()
                .salesOrder(order)
                .productId(product.getId())
                .quantity(-quantity)  // отрицательное количество для возврата
                .price(price)
                .productName(product.getName())
                .productSku(product.getSku())
                .notes(reason)
                .build();
    }

    /**
     * Обновляет количество в позиции
     */
    public SalesOrderItem updateQuantity(SalesOrderItem item, Integer newQuantity) {
        item.setQuantity(newQuantity);
        item.calculateTotal();
        return item;
    }

    /**
     * Обновляет цену в позиции
     */
    public SalesOrderItem updatePrice(SalesOrderItem item, BigDecimal newPrice) {
        item.setPrice(newPrice);
        item.calculateTotal();
        return item;
    }

    /**
     * Обновляет и количество, и цену
     */
    public SalesOrderItem updateItem(SalesOrderItem item, Integer newQuantity, BigDecimal newPrice) {
        if (newQuantity != null) {
            item.setQuantity(newQuantity);
        }
        if (newPrice != null) {
            item.setPrice(newPrice);
        }
        item.calculateTotal();
        return item;
    }
}