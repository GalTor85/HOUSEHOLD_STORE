package ru.galtor85.household_store.validator.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.stock.SameWarehouseTransferException;
import ru.galtor85.household_store.dto.request.stock.StockTransferRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.cell.CellValidationHelper;

/**
 * Validator for stock transfer operations.
 * Ensures that transfer requests are valid and all referenced entities exist.
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockTransferValidator {

    private final StorageCellRepository storageCellRepository;
    private final ProductStockRepository productStockRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final CellValidationHelper cellValidationHelper;

    /**
     * Validates that a source storage cell exists if specified.
     *
     * @param cellId      cell identifier (optional)
     * @param cellCode    cell code (optional)
     * @param warehouseId warehouse identifier (required for cellCode lookup)
     * @return storage cell entity or null if neither cellId nor cellCode provided
     * @throws IllegalArgumentException if cell is specified but not found
     */
    public StorageCell validateSourceCell(Long cellId, String cellCode, Long warehouseId) {
        return findStorageCell(cellId, cellCode, warehouseId);
    }

    /**
     * Validates that a destination storage cell exists if specified.
     *
     * @param cellId      cell identifier (optional)
     * @param cellCode    cell code (optional)
     * @param warehouseId warehouse identifier (required for cellCode lookup)
     * @return storage cell entity or null if neither cellId nor cellCode provided
     * @throws IllegalArgumentException if cell is specified but not found
     */
    public StorageCell validateDestinationCell(Long cellId, String cellCode, Long warehouseId) {
        return findStorageCell(cellId, cellCode, warehouseId);
    }

    /**
     * Finds a storage cell by ID or by code and warehouse ID.
     *
     * @param cellId      cell identifier (optional)
     * @param cellCode    cell code (optional)
     * @param warehouseId warehouse identifier (required for cellCode lookup)
     * @return storage cell entity or null
     */
    private StorageCell findStorageCell(Long cellId, String cellCode, Long warehouseId) {
        if (cellId != null) {
            return storageCellRepository.findById(cellId)
                    .orElseThrow(() -> {
                        log.error(logMsg.get("cell.not.found.id", cellId));
                        return new IllegalArgumentException(
                                messageService.get("cell.not.found.id", cellId));
                    });
        }
        if (cellCode != null && warehouseId != null) {
            return storageCellRepository.findByCodeAndWarehouseId(cellCode, warehouseId)
                    .orElseThrow(() -> {
                        log.error(logMsg.get("cell.not.found.code", cellCode, warehouseId));
                        return new IllegalArgumentException(
                                messageService.get("cell.not.found.code", cellCode, warehouseId));
                    });
        }
        return null;
    }

    /**
     * Validates that there is sufficient stock in the source warehouse.
     *
     * @param product     product entity
     * @param warehouseId source warehouse identifier
     * @param quantity    requested quantity to transfer
     * @throws InsufficientStockException if stock is insufficient
     */
    public void validateSufficientStock(Product product, Long warehouseId, int quantity) {
        ProductStock stock = productStockRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                .orElse(null);

        if (stock == null || stock.getQuantity() < quantity) {
            int available = stock != null ? stock.getQuantity() : 0;
            log.error(logMsg.get("stock.transfer.insufficient.stock",
                    product.getId(), warehouseId, available, quantity));
            throw new InsufficientStockException(product.getName(), available);
        }
    }

    /**
     * Validates the transfer request for logical consistency.
     *
     * @param request transfer request
     * @throws SameWarehouseTransferException if source and destination warehouses are the same
     */
    public void validateTransferRequest(StockTransferRequest request) {
        if (request.getFromWarehouseId() != null &&
                request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new SameWarehouseTransferException(request.getFromWarehouseId());
        }
    }

    /**
     * Validates that source cell contains the correct product.
     */
    public void validateSourceCellContainsProduct(StorageCell cell, Long productId, int quantity) {
        if (cell == null) {
            return;
        }

        if (!cell.getIsOccupied()) {
            throw new IllegalArgumentException(
                    messageService.get("stock.transfer.source.cell.empty", cell.getCode())
            );
        }

        if (!cell.getCurrentProductId().equals(productId)) {
            throw new IllegalArgumentException(
                    messageService.get("stock.transfer.source.cell.wrong.product",
                            cell.getCode(), productId)
            );
        }

        int currentQuantity = cell.getCurrentQuantity() != null ? cell.getCurrentQuantity() : 0;
        if (currentQuantity < quantity) {
            throw new IllegalArgumentException(
                    messageService.get("stock.transfer.source.cell.insufficient.quantity",
                            cell.getCode(), currentQuantity, quantity)
            );
        }
    }

    public void validateDestinationCellCapacity(StorageCell cell, Product product, int quantity) {
        if (cell == null) return;

        cellValidationHelper.validateCellActive(cell);
        cellValidationHelper.validateCellTypeCompatibility(cell, product);
        cellValidationHelper.validateWeightLimit(cell, product, quantity);
        cellValidationHelper.validateVolumeLimit(cell, product, quantity);

        if (cell.getIsOccupied() && !cell.getCurrentProductId().equals(product.getId())) {
            throw new IllegalArgumentException(
                    messageService.get("stock.transfer.dest.cell.different.product",
                            cell.getCode(), product.getSku())
            );
        }
    }

    /**
     * Validates that cell belongs to warehouse.
     */
    public void validateCellBelongsToWarehouse(StorageCell cell, Long warehouseId, String cellType) {
        if (cell == null) {
            return;
        }

        if (!cell.getWarehouse().getId().equals(warehouseId)) {
            throw new IllegalArgumentException(
                    messageService.get("stock.transfer.cell.not.in.warehouse",
                            cell.getCode(), cellType)
            );
        }
    }
}