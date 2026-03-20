package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.ProductStockDto;
import ru.galtor85.household_store.entity.ProductStock;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.repository.ProductStockRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.StockDtoEnricher;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseStockProcessor {

    private final ProductStockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockDtoEnricher dtoEnricher;
    private final MessageService messageService;

    private static final Set<String> PRODUCT_SORT_FIELDS = Set.of(
            "productName", "category", "sku", "brand", "price"
    );

    @Transactional(readOnly = true)
    public Page<ProductStockDto> getStockByWarehouse(Long warehouseId,
                                                     int page, int size,
                                                     String sortBy, String sortDir
                                                     ) {

        Page<ProductStock> stocks;
        Pageable pageable = PageRequest.of(page, size);

        if (PRODUCT_SORT_FIELDS.contains(sortBy)) {
            // Сортировка по полям продукта через join
            stocks = stockRepository.findByWarehouseIdWithSort(warehouseId, sortBy, sortDir, pageable);
        } else {
            // Обычная сортировка по полям ProductStock
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            pageable = PageRequest.of(page, size, sort);
            stocks = stockRepository.findByWarehouseId(warehouseId, pageable);
        }

        log.debug(messageService.get("stock.by.warehouse.fetched",
                stocks.getTotalElements(), warehouseId));

        return stocks.map(stock -> dtoEnricher.enrichStockDto(stock));
    }

    @Transactional(readOnly = true)
    public List<ProductStock> getLowStockItems(Long warehouseId) {
        List<ProductStock> lowStockItems = stockRepository.findLowStockItems(warehouseId);

        log.debug(messageService.get("stock.low.items.fetched",
                lowStockItems.size(), warehouseId));

        return lowStockItems;
    }

    @Transactional(readOnly = true)
    public Page<ProductStock> searchStockOnWarehouse(Long warehouseId, String searchTerm,
                                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.searchOnWarehouse(warehouseId, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public WarehouseStockSummary getWarehouseSummary(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        List<ProductStock> stocks = stockRepository.findByWarehouseId(warehouseId);

        return new WarehouseStockSummary(warehouse, stocks);
    }

    @lombok.Value
    public static class WarehouseStockSummary {
        Warehouse warehouse;
        List<ProductStock> stocks;
    }
}