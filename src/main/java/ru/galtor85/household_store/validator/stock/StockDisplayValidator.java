package ru.galtor85.household_store.validator.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for stock display operations.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDisplayValidator {

    private final ProductRepository productRepository;
    private final MessageService messageService;

    /**
     * Validates that a product exists.
     *
     * @param productId product identifier
     * @return product entity
     * @throws ProductNotFoundException if product not found
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn(messageService.get("stock.validator.product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }
}