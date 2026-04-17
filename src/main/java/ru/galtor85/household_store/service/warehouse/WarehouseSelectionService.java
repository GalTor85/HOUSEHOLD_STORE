package ru.galtor85.household_store.service.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.dto.response.stock.WarehouseStockDetailDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.stock.StockDisplayService;
import ru.galtor85.household_store.service.stock.StockService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service for selecting optimal warehouse for product placement and reservation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseSelectionService {

    private final StockService stockService;
    private final StockDisplayService stockDisplayService;
    private final ProductRepository productRepository;
    private final WarehouseConfig warehouseConfig;
    private final LogMessageService logMsg;

    /**
     * Selects optimal warehouse for reserving products from a sales order.
     */
    public Long selectWarehouseForReservation(SalesOrder order) {
        // Single product - optimize for that product
        if (order.getItems().size() == 1) {
            SalesOrderItem item = order.getItems().getFirst();
            return selectWarehouseForProduct(item.getProductId(), item.getQuantity());
        }

        // Try to find warehouse with all products
        Long warehouseWithAll = findWarehouseWithAllProducts(order);
        if (warehouseWithAll != null) {
            log.debug(logMsg.get("warehouse.selection.all.products",
                    warehouseWithAll, order.getId()));
            return warehouseWithAll;
        }

        // Check preferred warehouses from product settings
        Long preferredWarehouse = findPreferredWarehouse(order);
        if (preferredWarehouse != null) {
            log.debug(logMsg.get("warehouse.selection.preferred",
                    preferredWarehouse, order.getId()));
            return preferredWarehouse;
        }

        // Fallback to default
        log.warn(logMsg.get("warehouse.selection.fallback.default", order.getId()));
        return warehouseConfig.getDefaultWarehouseId();
    }

    /**
     * Selects optimal warehouse for receiving a product.
     */
    public Long selectWarehouseForReceiving(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return warehouseConfig.getDefaultWarehouseId();
        }

        // Use product's preferred warehouse if set
        if (product.getWarehouseId() != null) {
            log.debug(logMsg.get("warehouse.selection.product.preferred",
                    product.getWarehouseId(), productId));
            return product.getWarehouseId();
        }

        // Find warehouse that already has this product
        List<Warehouse> warehousesWithProduct = stockService.findWarehousesWithProduct(productId);
        if (!warehousesWithProduct.isEmpty()) {
            Long warehouseId = warehousesWithProduct.getFirst().getId();
            log.debug(logMsg.get("warehouse.selection.existing.product",
                    warehouseId, productId));
            return warehouseId;
        }

        // Use default warehouse
        return warehouseConfig.getDefaultWarehouseId();
    }

    /**
     * Selects optimal warehouse for a single product based on available stock.
     */
    public Long selectWarehouseForProduct(Long productId, int requiredQuantity) {
        List<Warehouse> warehouses = stockService.findWarehousesWithProduct(productId);

        if (warehouses.isEmpty()) {
            log.warn(logMsg.get("warehouse.selection.no.stock", productId));
            return warehouseConfig.getDefaultWarehouseId();
        }

        // Find warehouse with sufficient stock
        for (Warehouse warehouse : warehouses) {
            WarehouseStockDetailDto stock = getStockDetail(productId, warehouse.getId());
            if (stock.getAvailableQuantity() >= requiredQuantity) {
                log.debug(logMsg.get("warehouse.selection.sufficient",
                        warehouse.getId(), productId, stock.getAvailableQuantity(), requiredQuantity));
                return warehouse.getId();
            }
        }

        // Return warehouse with maximum available stock
        return warehouses.stream()
                .max(Comparator.comparing(w -> getStockDetail(productId, w.getId()).getAvailableQuantity()))
                .map(Warehouse::getId)
                .orElse(warehouseConfig.getDefaultWarehouseId());
    }

    private Long findWarehouseWithAllProducts(SalesOrder order) {
        List<WarehouseDto> warehouses = stockDisplayService.getWarehousesForSale(false);

        for (WarehouseDto warehouse : warehouses) {
            if (hasAllProductsWithSufficientStock(order, warehouse.getId())) {
                log.info(logMsg.get("warehouse.selection.found.all",
                        warehouse.getId(), order.getId(), order.getItems().size()));
                return warehouse.getId();
            }
        }

        return null;
    }

    private boolean hasAllProductsWithSufficientStock(SalesOrder order, Long warehouseId) {
        for (SalesOrderItem item : order.getItems()) {
            WarehouseStockDetailDto stock = getStockDetail(item.getProductId(), warehouseId);
            if (stock.getAvailableQuantity() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private Long findPreferredWarehouse(SalesOrder order) {
        return order.getItems().stream()
                .map(item -> productRepository.findById(item.getProductId())
                        .map(Product::getWarehouseId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private WarehouseStockDetailDto getStockDetail(Long productId, Long warehouseId) {
        return stockDisplayService.getProductAvailabilityForManager(productId, true)
                .getWarehouses().stream()
                .filter(w -> w.getWarehouseId().equals(warehouseId))
                .findFirst()
                .orElse(WarehouseStockDetailDto.builder()
                        .warehouseId(warehouseId)
                        .availableQuantity(0)
                        .build());
    }
}