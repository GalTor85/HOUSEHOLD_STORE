package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockProcessor {

    private final ProductRepository productRepository;
    private final MessageService messageService;

    public void restoreStockForCancelledOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int newQuantity = product.getQuantityInStock() + item.getQuantity();
                product.setQuantityInStock(newQuantity);
                productRepository.save(product);

                log.debug(messageService.get(
                        "manager.order.log.stock.restored",
                        item.getQuantity(),
                        item.getProductId()
                ));
            });
        }
    }

    public void checkAndReserveStock(Long productId, int requestedQuantity) {
        // Можно добавить логику резервирования товара
    }
}