package ru.galtor85.household_store.builder;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.entity.Product;

import java.math.BigDecimal;

@Component
public class OrderItemBuilder {

    public OrderItem buildFromProduct(Long productId, Integer quantity,
                                      BigDecimal price, Product product) {
        return OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .productName(product.getName())
                .productSku(product.getSku())
                .build();
    }

    public OrderItem buildFromProductWithCustomPrice(Long productId, Integer quantity,
                                                     BigDecimal customPrice, Product product) {
        BigDecimal price = customPrice != null ? customPrice : product.getPrice();
        return buildFromProduct(productId, quantity, price, product);
    }
}