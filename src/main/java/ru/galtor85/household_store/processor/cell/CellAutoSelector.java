package ru.galtor85.household_store.processor.cell;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.cell.NoAvailableCellException;
import ru.galtor85.household_store.advice.exception.cell.NoSuitableCellException;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CellAutoSelector {

    private final StorageCellRepository storageCellRepository;
    private final CellValidationHelper validationHelper;
    private final MessageService messageService;

    /**
     * Автоматический подбор подходящей ячейки для товара
     */
    public StorageCell selectCellForProduct(Long warehouseId, Product product, int quantity) {
        log.debug(messageService.get("cell.selector.start",
                product.getId(), quantity, warehouseId));

        // 1. Определяем требуемый тип ячейки
        CellType requiredType = validationHelper.determineRequiredCellType(product);
        log.debug(messageService.get("cell.selector.required.type", requiredType));

        // 2. Ищем свободные ячейки нужного типа
        List<StorageCell> availableCells = storageCellRepository
                .findAvailableCellsByType(warehouseId, requiredType);

        if (availableCells.isEmpty()) {
            log.error(messageService.get("cell.error.no.available",
                    warehouseId, requiredType));
            throw new NoAvailableCellException(warehouseId, requiredType);
        }

        log.debug(messageService.get("cell.selector.available.count",
                availableCells.size()));

        // 3. Фильтруем по вместимости
        List<StorageCell> suitableCells = availableCells.stream()
                .filter(cell -> isCellSuitable(cell, product, quantity))
                .collect(Collectors.toList());

        if (suitableCells.isEmpty()) {
            log.error(messageService.get("cell.error.no.suitable",
                    warehouseId, requiredType, product.getId()));
            throw new NoSuitableCellException(warehouseId, requiredType, product.getId());
        }

        log.debug(messageService.get("cell.selector.suitable.count",
                suitableCells.size()));

        // 4. Выбираем оптимальную ячейку
        StorageCell selectedCell = selectOptimalCell(suitableCells, product, quantity);

        log.info(messageService.get("cell.selector.selected",
                selectedCell.getCode(), selectedCell.getId()));

        return selectedCell;
    }

    /**
     * Проверка, подходит ли ячейка по весу и объему
     */
    private boolean isCellSuitable(StorageCell cell, Product product, int quantity) {
        // Проверка веса
        if (cell.getMaxWeightKg() != null && product.getWeightKg() != null) {
            double totalWeight = product.getWeightKg() * quantity;
            if (totalWeight > cell.getMaxWeightKg()) {
                log.debug(messageService.get("cell.selector.weight.exceeded",
                        cell.getCode(), totalWeight, cell.getMaxWeightKg()));
                return false;
            }
        }

        // Проверка объема
        if (cell.getMaxVolumeM3() != null && product.getVolumeM3() != null) {
            double totalVolume = product.getVolumeM3() * quantity;
            if (totalVolume > cell.getMaxVolumeM3()) {
                log.debug(messageService.get("cell.selector.volume.exceeded",
                        cell.getCode(), totalVolume, cell.getMaxVolumeM3()));
                return false;
            }
        }

        return true;
    }

    /**
     * Выбор оптимальной ячейки
     * Приоритет:
     * 1. Пустые ячейки
     * 2. Ячейки с тем же товаром
     * 3. Ячейки с минимальным заполнением
     */
    private StorageCell selectOptimalCell(List<StorageCell> cells, Product product, int quantity) {
        // Сначала ищем пустые ячейки
        List<StorageCell> emptyCells = cells.stream()
                .filter(cell -> !cell.getIsOccupied())
                .collect(Collectors.toList());

        if (!emptyCells.isEmpty()) {
            log.debug(messageService.get("cell.selector.using.empty", emptyCells.size()));
            // Сортируем по коду для консистентности
            return emptyCells.stream()
                    .sorted(Comparator.comparing(StorageCell::getCode))
                    .findFirst()
                    .orElse(null);
        }

        // Затем ищем ячейки с тем же товаром
        List<StorageCell> sameProductCells = cells.stream()
                .filter(cell -> cell.getIsOccupied() &&
                        product.getId().equals(cell.getCurrentProductId()))
                .collect(Collectors.toList());

        if (!sameProductCells.isEmpty()) {
            log.debug(messageService.get("cell.selector.using.same.product",
                    sameProductCells.size()));
            return sameProductCells.stream()
                    .sorted(Comparator.comparing(StorageCell::getCode))
                    .findFirst()
                    .orElse(null);
        }

        // В итоге берем любую ячейку с минимальным заполнением
        log.debug(messageService.get("cell.selector.using.partial"));
        return cells.stream()
                .min(Comparator.comparing(StorageCell::getCurrentQuantity))
                .orElse(null);
    }

    /**
     * Получить все свободные ячейки определенного типа
     */
    public List<StorageCell> getAvailableCellsByType(Long warehouseId, CellType cellType) {
        return storageCellRepository.findAvailableCellsByType(warehouseId, cellType);
    }

    /**
     * Проверить, есть ли свободные ячейки для продукта
     */
    public boolean hasAvailableCell(Long warehouseId, Product product, int quantity) {
        try {
            selectCellForProduct(warehouseId, product, quantity);
            return true;
        } catch (NoAvailableCellException | NoSuitableCellException e) {
            return false;
        }
    }
}