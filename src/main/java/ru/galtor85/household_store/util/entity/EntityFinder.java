package ru.galtor85.household_store.util.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

/**
 * Utility for finding entities by ID with consistent error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityFinder {

    private final SupplierRepository supplierRepository;
    private final LogMessageService logMsg;
    private final ProductValidator productValidator;

    /**
     * Finds supplier by ID.
     *
     * @param id supplier ID
     * @throws SupplierNotFoundException if not found
     */
    public void findSupplierById(Long id) {
        supplierRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(logMsg.get("manager.supplier.log.not.found", id));
                    return new SupplierNotFoundException(id);
                });
    }

    /**
     * Finds product by ID.
     *
     * @param id product ID
     * @return product entity
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException if not found
     */
    public Product findProductById(Long id) {
        return productValidator.validateProductExists(id);
    }
}