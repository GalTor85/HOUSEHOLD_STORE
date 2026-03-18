package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductStock;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.mapper.ProductStockMapper;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.ProductStockRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductStockRepository stockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductStockMapper stockMapper;
    private final MessageService messageService;

    // ========== ПРОСМОТР ОСТАТКОВ ПО СКЛАДУ ==========

    /**
     * Получить все остатки на конкретном складе с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<ProductStockDto> getStockByWarehouse(Long warehouseId,
                                                     int page, int size,
                                                     String sortBy, String sortDir,
                                                     Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем существование склада
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }

        // Поля для сортировки по продукту
        Set<String> productSortFields = Set.of("productName", "category", "sku", "brand", "price");

        Page<ProductStock> stocks;
        Pageable pageable = PageRequest.of(page, size);

        if (productSortFields.contains(sortBy)) {
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

        Locale finalLocale = locale;
        return stocks.map(stock -> enrichStockDto(stock, finalLocale));
    }

    // ========== ПРОСМОТР ПО ПРОДУКТУ (ВСЕ СКЛАДЫ) ==========

    /**
     * Получить остатки по конкретному товару на ВСЕХ складах
     */
    @Transactional(readOnly = true)
    public List<ProductStockDto> getProductStockAcrossAllWarehouses(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        List<ProductStock> stocks = stockRepository.findByProductId(productId);

        log.debug(messageService.get("stock.by.product.all.warehouses",
                stocks.size(), productId));

        Locale finalLocale = locale;
        return stocks.stream()
                .map(stock -> enrichStockDto(stock, finalLocale))
                .collect(Collectors.toList());
    }

    /**
     * Получить общее количество товара на всех складах
     */
    @Transactional(readOnly = true)
    public Integer getTotalStockForProduct(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Integer total = stockRepository.getTotalStockForProduct(productId);
        total = total != null ? total : 0;

        log.debug(messageService.get("stock.product.total", productId, total));

        return total;
    }

    /**
     * Получить детальную информацию по товару с разбивкой по складам
     */
    @Transactional(readOnly = true)
    public ProductStockDistributionDto getProductStockDistribution(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<ProductStock> stocks = stockRepository.findByProductId(productId);

        int totalQuantity = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
        int totalReserved = stocks.stream()
                .mapToInt(s -> s.getReservedQuantity() != null ? s.getReservedQuantity() : 0)
                .sum();
        int totalAvailable = totalQuantity - totalReserved;

        List<WarehouseStockDto> warehouseStocks = stocks.stream()
                .map(stock -> {
                    Warehouse warehouse = warehouseRepository.findById(stock.getWarehouseId()).orElse(null);
                    return WarehouseStockDto.builder()
                            .warehouseId(stock.getWarehouseId())
                            .warehouseName(warehouse != null ? warehouse.getName() : "Unknown")
                            .quantity(stock.getQuantity())
                            .reservedQuantity(stock.getReservedQuantity())
                            .availableQuantity(stock.getQuantity() -
                                    (stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0))
                            .percentage(totalQuantity > 0 ?
                                    (stock.getQuantity() * 100.0 / totalQuantity) : 0)
                            .build();
                })
                .collect(Collectors.toList());

        return ProductStockDistributionDto.builder()
                .productId(productId)
                .productName(product.getName())
                .productSku(product.getSku())
                .totalQuantity(totalQuantity)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .warehouses(warehouseStocks)
                .build();
    }

    // ========== ПРОСМОТР ПО КОНКРЕТНОМУ СКЛАДУ И ПРОДУКТУ ==========

    /**
     * Получить остаток конкретного товара на конкретном складе
     */
    @Transactional(readOnly = true)
    public ProductStockDto getProductStockAtWarehouse(Long productId, Long warehouseId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        ProductStock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElse(ProductStock.builder()
                        .productId(productId)
                        .warehouseId(warehouseId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .availableQuantity(0)
                        .build());

        return enrichStockDto(stock, locale);
    }

    // ========== ПРОСМОТР ПО ВСЕМ СКЛАДАМ ==========

    /**
     * Получить сводку по всем складам
     */
    @Transactional(readOnly = true)
    public List<WarehouseStockSummaryDto> getAllWarehousesSummary(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<WarehouseStockSummaryDto> summaries = new ArrayList<>();

        for (Warehouse warehouse : warehouses) {
            summaries.add(getWarehouseSummary(warehouse.getId(), locale));
        }

        log.debug(messageService.get("stock.all.warehouses.summary", warehouses.size()));

        return summaries;
    }

    /**
     * Получить детальную сводку по конкретному складу
     */
    @Transactional(readOnly = true)
    public WarehouseStockSummaryDto getWarehouseSummary(Long warehouseId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));

        List<ProductStock> stocks = stockRepository.findByWarehouseId(warehouseId);

        int totalProducts = stocks.size();
        int totalItems = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
        double totalValue = calculateTotalValue(stocks);
        int lowStockCount = (int) stocks.stream()
                .filter(s -> s.getQuantity() < (s.getMinStockLevel() != null ? s.getMinStockLevel() : 0))
                .count();
        int outOfStockCount = (int) stocks.stream()
                .filter(s -> s.getQuantity() == 0)
                .count();

        // Топ-5 товаров по стоимости на складе - ИСПОЛЬЗУЕМ ОТДЕЛЬНЫЙ КЛАСС
        List<TopProductDto> topProducts = stocks.stream()
                .map(s -> {
                    Product p = productRepository.findById(s.getProductId()).orElse(null);
                    double value = p != null && p.getPrice() != null ?
                            p.getPrice().doubleValue() * s.getQuantity() : 0;
                    return new TopProductDto(
                            s.getProductId(),
                            p != null ? p.getName() : "Unknown",
                            s.getQuantity(),
                            value
                    );
                })
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .collect(Collectors.toList());

        return WarehouseStockSummaryDto.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouse.getName())
                .totalProducts(totalProducts)
                .totalItems(totalItems)
                .totalValue(totalValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .utilizationPercentage(calculateUtilization(warehouse, stocks))
                .topProducts(topProducts)  // ТЕПЕРЬ РАБОТАЕТ
                .build();
    }

    private double calculateUtilization(Warehouse warehouse, List<ProductStock> stocks) {
        if (warehouse.getTotalCapacity() == null || warehouse.getTotalCapacity() == 0) {
            return 0.0;
        }

        // Примерная оценка занятости на основе количества товаров
        double totalItems = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
        return (totalItems / warehouse.getTotalCapacity()) * 100;
    }


    // ========== ПОИСК И ФИЛЬТРАЦИЯ ==========

    /**
     * Поиск товаров с низким остатком на складе
     */
    @Transactional(readOnly = true)
    public List<ProductStockDto> getLowStockItems(Long warehouseId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<ProductStock> lowStockItems = stockRepository.findLowStockItems(warehouseId);

        log.debug(messageService.get("stock.low.items.fetched",
                lowStockItems.size(), warehouseId));

        Locale finalLocale = locale;
        return lowStockItems.stream()
                .map(stock -> enrichStockDto(stock, finalLocale))
                .collect(Collectors.toList());
    }

    /**
     * Поиск товаров по названию или SKU на складе
     */
    @Transactional(readOnly = true)
    public Page<ProductStockDto> searchStockOnWarehouse(Long warehouseId, String searchTerm,
                                                        int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Pageable pageable = PageRequest.of(page, size);

        // Кастомный запрос с join к Product для поиска по имени и SKU
        Page<ProductStock> stocks = stockRepository.searchOnWarehouse(
                warehouseId, searchTerm, pageable);

        Locale finalLocale = locale;
        return stocks.map(stock -> enrichStockDto(stock, finalLocale));
    }

    /**
     * Найти все склады, где есть конкретный товар
     */
    @Transactional(readOnly = true)
    public List<Warehouse> findWarehousesWithProduct(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<ProductStock> stocks = stockRepository.findByProductId(productId);

        List<Long> warehouseIds = stocks.stream()
                .map(ProductStock::getWarehouseId)
                .collect(Collectors.toList());

        return warehouseRepository.findAllById(warehouseIds);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private ProductStockDto enrichStockDto(ProductStock stock, Locale locale) {
        Product product = productRepository.findById(stock.getProductId()).orElse(null);
        Warehouse warehouse = warehouseRepository.findById(stock.getWarehouseId()).orElse(null);

        int reserved = stock.getReservedQuantity() != null ? stock.getReservedQuantity() : 0;
        int available = stock.getQuantity() - reserved;
        boolean isLowStock = stock.getMinStockLevel() != null && stock.getQuantity() < stock.getMinStockLevel();

        double stockValue = 0.0;
        if (product != null && product.getPrice() != null) {
            stockValue = product.getPrice().doubleValue() * stock.getQuantity();
        }

        return ProductStockDto.builder()
                .id(stock.getId())
                .productId(stock.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(available)
                .minStockLevel(stock.getMinStockLevel())
                .maxStockLevel(stock.getMaxStockLevel())
                .reorderPoint(stock.getReorderPoint())
                .locationInWarehouse(stock.getLocationInWarehouse())
                .isLowStock(isLowStock)
                .stockValue(stockValue)
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    private double calculateTotalValue(List<ProductStock> stocks) {
        double total = 0.0;
        for (ProductStock stock : stocks) {
            Product product = productRepository.findById(stock.getProductId()).orElse(null);
            if (product != null && product.getPrice() != null) {
                total += product.getPrice().doubleValue() * stock.getQuantity();
            }
        }
        return total;
    }


}