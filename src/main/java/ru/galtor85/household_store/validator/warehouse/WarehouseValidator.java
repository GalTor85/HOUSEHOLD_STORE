package ru.galtor85.household_store.validator.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseValidator {

    private final WarehouseRepository warehouseRepository;
    private final MessageService messageService;

    public Warehouse validateWarehouseExists(Long warehouseId) {
        if (warehouseId == null) {
            log.error(messageService.get("receive.validation.warehouse.id.null"));
            throw new IllegalArgumentException(
                    messageService.get("receive.validation.warehouse.id.null")
            );
        }

        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.error.not.found.id", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });
    }

    public void validateAnyWarehouseExists() {
        if (warehouseRepository.count() == 0) {
            log.error(messageService.get("warehouse.error.no.warehouses"));
            throw new WarehouseNotFoundException();
        }
    }

    public void validateWarehouseCodeUnique(String code) {
        if (warehouseRepository.existsByCode(code)) {
            log.warn(messageService.get("warehouse.log.code.exists", code));
            throw new WarehouseAlreadyExistsException("code", code);
        }
    }

    public void validateWarehouseBarcodeUnique(String barcode) {
        if (warehouseRepository.existsByBarcode(barcode)) {
            log.warn(messageService.get("warehouse.log.barcode.exists", barcode));
            throw new WarehouseAlreadyExistsException("barcode", barcode);
        }
    }

    public void validateWarehouseSearchResult(boolean hasResults, String search) {
        if (!hasResults) {
            log.warn(messageService.get("warehouse.search.not.found", search));
            throw new WarehouseNotFoundException(search);
        }
    }

    public void validateOrderHasItems(SalesOrder salesOrder) {
        if (salesOrder.getItems() == null || salesOrder.getItems().isEmpty()) {
            log.warn(messageService.get("warehouse.resolver.no.items", salesOrder.getId()));

            throw new IllegalArgumentException(
                 messageService.get("warehouse.resolver.no.items.error", salesOrder.getId())
            );
        }
    }
}