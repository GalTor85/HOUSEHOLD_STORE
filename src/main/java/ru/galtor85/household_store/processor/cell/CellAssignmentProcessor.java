package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.stock.StockMovementBuilder;
import ru.galtor85.household_store.entity.stock.MovementType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.stock.StockMovement;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.stock.StockMovementRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellAssignmentProcessor {

    private final StorageCellRepository storageCellRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CellValidationHelper validationHelper;
    private final StockMovementBuilder movementBuilder;
    private final MessageService messageService;

    @Transactional
    public StorageCell assignProductToCell(StorageCell cell, Product product,
                                           int quantity, Long assignedBy) {
        // Валидации
        validationHelper.validateCellActive(cell);
        validationHelper.validateCellNotOccupied(cell);
        validationHelper.validateCellTypeCompatibility(cell, product);
        validationHelper.validateWeightLimit(cell, product, quantity);
        validationHelper.validateVolumeLimit(cell, product, quantity);

        // Назначение товара
        cell.setCurrentProductId(product.getId());
        cell.setCurrentQuantity(quantity);
        cell.setIsOccupied(true);
        cell.setLastInventoryDate(java.time.LocalDateTime.now());

        StorageCell updatedCell = storageCellRepository.save(cell);

        // Создание движения
        StockMovement movement = movementBuilder.buildMovement(
                product.getId(), null, cell.getId(), quantity,
                MovementType.RECEIPT, "ASSIGN", assignedBy);
        stockMovementRepository.save(movement);

        log.info(messageService.get("cell.log.product.assigned",
                product.getId(), cell.getId(), quantity, assignedBy));

        return updatedCell;
    }
}