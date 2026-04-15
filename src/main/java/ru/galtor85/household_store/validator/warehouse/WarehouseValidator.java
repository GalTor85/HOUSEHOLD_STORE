package ru.galtor85.household_store.validator.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for warehouse operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseValidator {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final WarehouseRepository warehouseRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    // =========================================================================
    // WAREHOUSE EXISTENCE VALIDATION
    // =========================================================================

    /**
     * Validates that a warehouse exists by ID
     *
     * @param warehouseId warehouse identifier
     * @return warehouse entity
     * @throws IllegalArgumentException if warehouseId is null
     * @throws WarehouseNotFoundException if warehouse not found
     */
    public Warehouse validateWarehouseExists(Long warehouseId) {
        if (warehouseId == null) {
            log.error(logMsg.get("receive.validation.warehouse.id.null"));
            throw new IllegalArgumentException(
                    messageService.get("receive.validation.warehouse.id.null")
            );
        }

        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("warehouse.error.not.found.id", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });
    }

    // =========================================================================
    // UNIQUENESS VALIDATION
    // =========================================================================

    /**
     * Validates that warehouse code is unique
     *
     * @param code warehouse code
     * @throws WarehouseAlreadyExistsException if code already exists
     */
    public void validateWarehouseCodeUnique(String code) {
        if (warehouseRepository.existsByCode(code)) {
            log.warn(logMsg.get("warehouse.log.code.exists", code));
            throw new WarehouseAlreadyExistsException("code", code);
        }
    }

    /**
     * Validates that warehouse barcode is unique
     *
     * @param barcode warehouse barcode
     * @throws WarehouseAlreadyExistsException if barcode already exists
     */
    public void validateWarehouseBarcodeUnique(String barcode) {
        if (warehouseRepository.existsByBarcode(barcode)) {
            log.warn(logMsg.get("warehouse.log.barcode.exists", barcode));
            throw new WarehouseAlreadyExistsException("barcode", barcode);
        }
    }

    // =========================================================================
    // SEARCH VALIDATION
    // =========================================================================

    /**
     * Validates that warehouse search returned results
     *
     * @param hasResults whether search has results
     * @param search search term
     * @throws WarehouseNotFoundException if no results found
     */
    public void validateWarehouseSearchResult(boolean hasResults, String search) {
        if (!hasResults) {
            log.warn(logMsg.get("warehouse.search.not.found", search));
            throw new WarehouseNotFoundException(search);
        }
    }

    // =========================================================================
    // ORDER VALIDATION
    // =========================================================================

    /**
     * Validates that a sales order has items (for warehouse resolution)
     *
     * @param salesOrder sales order entity
     * @throws IllegalArgumentException if order has no items
     */
    public void validateOrderHasItems(SalesOrder salesOrder) {
        if (salesOrder.getItems() == null || salesOrder.getItems().isEmpty()) {
            log.warn(logMsg.get("warehouse.resolver.no.items", salesOrder.getId()));

            throw new IllegalArgumentException(
                 messageService.get("warehouse.resolver.no.items.error", salesOrder.getId())
            );
        }
    }
}