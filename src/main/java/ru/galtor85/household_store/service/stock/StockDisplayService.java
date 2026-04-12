package ru.galtor85.household_store.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.StockDisplayConfig;
import ru.galtor85.household_store.converter.ProductAvailabilityConverter;
import ru.galtor85.household_store.dto.response.stock.ProductAvailabilityDto;
import ru.galtor85.household_store.dto.response.stock.ProductAvailabilityWithWarehousesDto;
import ru.galtor85.household_store.dto.response.stock.WarehouseStockDetailDto;
import ru.galtor85.household_store.dto.response.warehouse.WarehouseDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.warehouse.WarehouseMapper;
import ru.galtor85.household_store.processor.stock.StockAvailabilityProcessor;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.stock.StockDisplayValidator;
import ru.galtor85.household_store.validator.warehouse.WarehouseValidator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for displaying product stock information to customers and managers.
 *
 * <p>This service provides methods to get product availability with caching
 * and localized status messages. Supports both customer view (visible warehouses only)
 * and manager view (all warehouses with optional visibility filter).</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDisplayService {

    private final StockDisplayValidator validator;
    private final StockAvailabilityProcessor processor;
    private final ProductAvailabilityConverter converter;
    private final MessageService messageService;
    private final StockDisplayConfig config;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final ProductStockRepository productStockRepository;
    private final CacheManager cacheManager;
    private final WarehouseValidator warehouseValidator;
    private final ProductRepository productRepository;

    /* Cache duration in seconds */
    private static final int SECONDS_PER_MINUTE = 60;

    // =========================================================================
    // CUSTOMER METHODS (visible warehouses only)
    // =========================================================================

    /**
     * Gets product availability for customer view (visible warehouses only).
     * Results are cached to reduce database load.
     *
     * @param productId product identifier
     * @return product availability DTO
     */
    @Cacheable(value = "productAvailability", key = "#productId", unless = "#result == null")
    public ProductAvailabilityDto getProductAvailabilityForCustomer(Long productId) {
        log.debug(messageService.get("stock.display.service.customer.start", productId));

        Product product = validator.validateProductExists(productId);
        Integer availableStock = processor.calculateAvailableStockForCustomer(product);
        ProductAvailabilityDto result = converter.toDto(product, availableStock);

        log.debug(messageService.get("stock.display.service.customer.complete",
                productId, result.getStatus(), availableStock));

        return result;
    }

    /**
     * Gets all products with availability for customers.
     * Only includes stock from warehouses visible for sale.
     *
     * @param category category filter (optional)
     * @param page page number
     * @param size page size
     * @param sortBy sort field (name, price, etc.)
     * @param sortDir sort direction (asc/desc)
     * @return page of product availability DTOs
     */
    public Page<ProductAvailabilityDto> getAllProductsWithAvailability(String category, int page, int size,
                                                                       String sortBy, String sortDir) {
        log.debug(messageService.get("stock.display.service.products.start", page, size, category));

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryAndActiveTrue(category, pageable);
        } else {
            products = productRepository.findByActiveTrue(pageable);
        }

        Page<ProductAvailabilityDto> result = products.map(product -> {
            Integer availableStock = processor.calculateAvailableStockForCustomer(product);
            return converter.toDto(product, availableStock);
        });

        log.debug(messageService.get("stock.display.service.products.complete", result.getTotalElements()));

        return result;
    }

    // =========================================================================
    // MANAGER METHODS (all warehouses)
    // =========================================================================

    /**
     * Gets product availability for manager view (all warehouses).
     *
     * @param productId product identifier
     * @param includeInvisible whether to include warehouses marked as invisible for sale
     * @return product availability DTO with warehouse details
     */
    public ProductAvailabilityWithWarehousesDto getProductAvailabilityForManager(Long productId, boolean includeInvisible) {
        log.debug(messageService.get("stock.display.service.manager.start", productId, includeInvisible));

        Product product = validator.validateProductExists(productId);
        Integer availableStock = processor.calculateAvailableStock(product);
        List<WarehouseStockDetailDto> warehouseDetails = processor.getStockDetailsByWarehouse(productId, includeInvisible);

        ProductAvailabilityWithWarehousesDto result = ProductAvailabilityWithWarehousesDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .totalAvailable(availableStock)
                .warehouses(warehouseDetails)
                .build();

        log.debug(messageService.get("stock.display.service.manager.complete", productId, warehouseDetails.size()));

        return result;
    }

    /**
     * Gets warehouses for sale with visibility filter.
     *
     * @param includeInvisible whether to include warehouses marked as invisible for sale
     * @return list of warehouse DTOs
     */
    public List<WarehouseDto> getWarehousesForSale(boolean includeInvisible) {
        log.debug(messageService.get("stock.display.service.warehouses.start", includeInvisible));

        List<Warehouse> warehouses = warehouseRepository.findWarehousesForSale(includeInvisible);

        log.debug(messageService.get("stock.display.service.warehouses.complete", warehouses.size()));

        return warehouses.stream()
                .map(warehouseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Toggles warehouse visibility for sale.
     * When a warehouse is marked as invisible, customers cannot see stock from this warehouse.
     *
     * @param warehouseId warehouse ID
     * @param visible visibility status (true = visible, false = invisible)
     * @return updated warehouse DTO
     */
    @Transactional
    public WarehouseDto toggleWarehouseVisibility(Long warehouseId, boolean visible) {
        log.info(messageService.get("stock.display.service.visibility.start", warehouseId, visible));

        Warehouse warehouse = warehouseValidator.validateWarehouseExists(warehouseId);
        warehouse.setIsVisibleForSale(visible);
        warehouse = warehouseRepository.save(warehouse);

        // Clear cache for all products in this warehouse to reflect visibility change
        clearCacheForWarehouse(warehouseId);

        log.info(messageService.get("stock.display.service.visibility.complete", warehouseId, visible));

        return warehouseMapper.toDto(warehouse);
    }

    // =========================================================================
    // LEGACY METHOD (deprecated)
    // =========================================================================

    /**
     * Gets product availability (legacy method - uses all warehouses).
     *
     * @param productId product identifier
     * @return product availability DTO
     * @deprecated Use {@link #getProductAvailabilityForCustomer(Long)} instead
     */
    @Deprecated
    @Cacheable(value = "productAvailability", key = "#productId", unless = "#result == null")
    public ProductAvailabilityDto getProductAvailability(Long productId) {
        log.debug(messageService.get("stock.display.service.start", productId));

        Product product = validator.validateProductExists(productId);
        Integer availableStock = processor.calculateAvailableStock(product);
        ProductAvailabilityDto result = converter.toDto(product, availableStock);

        log.debug(messageService.get("stock.display.service.complete",
                productId, result.getStatus(), availableStock));

        return result;
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Clears cache for all products stored in a specific warehouse.
     *
     * @param warehouseId warehouse identifier
     */
    private void clearCacheForWarehouse(Long warehouseId) {
        List<ProductStock> stocks = productStockRepository.findByWarehouseId(warehouseId);
        for (ProductStock stock : stocks) {
            Objects.requireNonNull(cacheManager.getCache("productAvailability")).evict(stock.getProductId());
        }
        log.debug(messageService.get("stock.display.service.cache.cleared", warehouseId, stocks.size()));
    }

    /**
     * Gets cache duration in seconds.
     *
     * @return cache TTL in seconds
     */
    public int getCacheTtlSeconds() {
        return config.getCacheMinutes() * SECONDS_PER_MINUTE;
    }
}