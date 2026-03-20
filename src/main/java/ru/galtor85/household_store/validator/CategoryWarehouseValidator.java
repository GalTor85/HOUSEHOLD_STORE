package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.CategoryAlreadyAssignedException;
import ru.galtor85.household_store.advice.exception.CategoryNotFoundException;
import ru.galtor85.household_store.advice.exception.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.BulkCategoryWarehouseRequest;
import ru.galtor85.household_store.entity.CategoryWarehouse;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.repository.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryWarehouseValidator {

    private final WarehouseRepository warehouseRepository;
    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final MessageService messageService;

    public Warehouse validateWarehouseExists(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.log.not.found", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });
    }

    public void validateCategoryNotAssigned(String category) {
        if (categoryWarehouseRepository.findByCategory(category).isPresent()) {
            log.warn(messageService.get("category.log.already.assigned", category));
            throw new CategoryAlreadyAssignedException(category);
        }
    }

    public CategoryWarehouse validateCategoryExists(String category) {
        return categoryWarehouseRepository.findByCategory(category)
                .orElseThrow(() -> {
                    log.error(messageService.get("category.log.not.found", category));
                    return new CategoryNotFoundException(category);
                });
    }

    public void validateCategoriesForBulkAssign(BulkCategoryWarehouseRequest request) {
        if (request.getCategories() == null || request.getCategories().isEmpty()) {
            log.error(messageService.get("category.log.bulk.empty"));
            throw new IllegalArgumentException(messageService.get("category.log.bulk.empty"));
        }
    }
}