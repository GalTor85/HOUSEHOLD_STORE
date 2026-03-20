package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.CartEmptyException;
import ru.galtor85.household_store.advice.exception.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.entity.Cart;
import ru.galtor85.household_store.entity.CartItem;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final ProductRepository productRepository;
    private final MessageService messageService;

    public void validateCartNotEmpty(Cart cart, Long userId) {
        if (cart.getItems().isEmpty()) {
            log.warn(messageService.get("order.log.cart.empty", userId));
            throw new CartEmptyException();
        }
    }

    public void validateStockAvailability(Cart cart) {
        for (CartItem item : cart.getItems()) {
            Product product = findProductById(item.getProductId());
            validateProductStock(product, item.getQuantity());
        }
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("order.log.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    private void validateProductStock(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get(
                    "order.log.insufficient.stock",
                    product.getName(),
                    product.getQuantityInStock(),
                    requestedQuantity
            ));
            throw new InsufficientStockException(
                    product.getId() + "-" + product.getName() + "-" + product.getQuantityInStock(),
                    requestedQuantity
            );
        }
    }

    public void validateStockUpdate(Product product, int quantity) {
        int newQuantity = product.getQuantityInStock() - quantity;
        if (newQuantity < 0) {
            log.error(messageService.get(
                    "order.log.stock.negative",
                    product.getId(),
                    product.getQuantityInStock(),
                    quantity
            ));
            throw new InsufficientStockException(
                    product.getId() + "-" + product.getName() + "-" + product.getQuantityInStock(),
                    quantity
            );
        }
    }
}