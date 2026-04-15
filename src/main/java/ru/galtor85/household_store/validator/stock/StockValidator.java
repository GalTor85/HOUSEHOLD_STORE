package ru.galtor85.household_store.validator.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Validator for stock operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockValidator {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final LogMessageService logMsg;

    /**
     * Validates warehouse exists.
     *
     * @param warehouseId warehouse ID
     * @throws WarehouseNotFoundException if not found
     */
    public void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            log.error(logMsg.get("warehouse.not.found", warehouseId));
            throw new WarehouseNotFoundException(warehouseId);
        }
    }

    /**
     * Validates product exists.
     *
     * @param productId product ID
     * @throws ProductNotFoundException if not found
     */
    public void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            log.error(logMsg.get("product.not.found", productId));
            throw new ProductNotFoundException(productId);
        }
    }
}