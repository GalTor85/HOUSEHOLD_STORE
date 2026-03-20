package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.entity.Cart;
import ru.galtor85.household_store.entity.CartItem;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.validator.OrderValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockUpdateProcessor {

    private final ProductRepository productRepository;
    private final OrderValidator orderValidator;
    private final MessageService messageService;

    @Transactional
    public void updateStockForCart(Cart cart) {
        for (CartItem item : cart.getItems()) {
            updateProductStock(item.getProductId(), item.getQuantity());
        }
    }

    private void updateProductStock(Long productId, Integer quantity) {
        Product product = findProductById(productId);
        orderValidator.validateStockUpdate(product, quantity);

        int oldQuantity = product.getQuantityInStock();
        int newQuantity = oldQuantity - quantity;
        product.setQuantityInStock(newQuantity);

        productRepository.save(product);

        log.debug(messageService.get(
                "order.log.stock.updated",
                productId,
                oldQuantity,
                newQuantity
        ));
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("order.log.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }
}