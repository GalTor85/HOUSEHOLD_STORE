package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.WarehouseNotFoundException;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockValidator {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;

    public void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            log.error(messageService.get("warehouse.not.found", warehouseId));
            throw new WarehouseNotFoundException(warehouseId);
        }
    }

    public void validateProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            log.error(messageService.get("product.not.found", productId));
            throw new ProductNotFoundException(productId);
        }
    }
}