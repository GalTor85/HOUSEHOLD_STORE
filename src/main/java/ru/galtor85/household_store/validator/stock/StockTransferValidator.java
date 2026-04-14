package ru.galtor85.household_store.validator.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.stock.SameWarehouseTransferException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.request.stock.StockTransferRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.StorageCell;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.StorageCellRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for stock transfer operations.
 * Ensures that transfer requests are valid and all referenced entities exist.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockTransferValidator {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageCellRepository storageCellRepository;
    private final ProductStockRepository productStockRepository;
    private final MessageService messageService;

    /**
     * Validates that a product exists and returns it.
     *
     * @param productId product identifier
     * @return product entity
     * @throws ProductNotFoundException if product not found
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("product.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Validates that a source warehouse exists.
     * Returns null if warehouseId is null (source warehouse is optional).
     *
     * @param warehouseId source warehouse identifier
     * @return warehouse entity or null
     * @throws WarehouseNotFoundException if warehouseId is provided but not found
     */
    public Warehouse validateSourceWarehouseExists(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });
    }

    /**
     * Validates that a destination warehouse exists.
     *
     * @param warehouseId destination warehouse identifier
     * @return warehouse entity
     * @throws WarehouseNotFoundException if warehouse not found
     */
    public Warehouse validateDestinationWarehouseExists(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });
    }

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
                        log.error(messageService.get("cell.not.found.id", cellId));
                        return new IllegalArgumentException(
                                messageService.get("cell.not.found.id", cellId));
                    });
        }
        if (cellCode != null && warehouseId != null) {
            return storageCellRepository.findByCodeAndWarehouseId(cellCode, warehouseId)
                    .orElseThrow(() -> {
                        log.error(messageService.get("cell.not.found.code", cellCode, warehouseId));
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
            log.error(messageService.get("stock.transfer.insufficient.stock",
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
}