package ru.galtor85.household_store.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.warehouse.CategoryWarehouse;
import ru.galtor85.household_store.processor.warehouse.WarehousePriorityProcessor;
import ru.galtor85.household_store.processor.warehouse.WarehouseSelector;
import ru.galtor85.household_store.repository.category.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.validator.warehouse.WarehouseValidator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolver for determining the optimal warehouse for a sales order.
 *
 * <p>Analyzes products in a sales order and determines the most suitable
 * warehouse using a priority-based algorithm.</p>
 *
 * <p>Warehouse selection priority (highest to lowest):
 * <ol>
 *   <li>Direct warehouse assignment on the product</li>
 *   <li>Category-based warehouse assignment</li>
 *   <li>Default warehouse from configuration</li>
 * </ol>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseResolver {

    private final ProductRepository productRepository;
    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final WarehouseValidator validator;
    private final WarehousePriorityProcessor priorityProcessor;
    private final WarehouseSelector selector;

    /**
     * Resolves the optimal warehouse for a sales order.
     *
     * <p>Calculates warehouse priorities based on all products in the order
     * and selects the warehouse with the highest accumulated score.</p>
     *
     * @param salesOrder the sales order to resolve warehouse for
     * @return the resolved warehouse ID
     * @throws ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException if no warehouse found
     */
    public Long resolveWarehouseId(SalesOrder salesOrder) {
        validator.validateOrderHasItems(salesOrder);

        List<Product> products = getProductsFromSalesOrder(salesOrder);
        Map<Long, Integer> warehousePriorities = priorityProcessor.collectPriorities(
                products,
                this::getWarehouseForCategory
        );

        return selector.selectByPriority(warehousePriorities, salesOrder.getId());
    }

    /**
     * Resolves the warehouse with an optional forced selection.
     *
     * <p>If a forced warehouse ID is provided and valid, it will be used
     * regardless of priority calculations. Otherwise, falls back to
     * priority-based resolution.</p>
     *
     * @param salesOrder the sales order to resolve warehouse for
     * @param forcedWarehouseId optional forced warehouse ID (maybe null)
     * @return the resolved warehouse ID
     * @throws ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException if forced warehouse not found
     */
    public Long resolveWarehouseId(SalesOrder salesOrder, Long forcedWarehouseId) {
        Long forced = selector.selectForced(forcedWarehouseId, salesOrder);
        if (forced != null) {
            validator.validateWarehouseExists(forced);
            return forced;
        }
        return resolveWarehouseId(salesOrder);
    }

    /**
     * Retrieves all products referenced in the sales order.
     *
     * @param salesOrder the sales order containing product references
     * @return list of Product entities
     */
    private List<Product> getProductsFromSalesOrder(SalesOrder salesOrder) {
        List<Long> productIds = salesOrder.getItems().stream()
                .map(SalesOrderItem::getProductId)
                .collect(Collectors.toList());
        return productRepository.findAllById(productIds);
    }

    /**
     * Resolves the warehouse ID for a product category.
     *
     * <p>Resolution strategy:
     * <ol>
     *   <li>Return default warehouse for the category if configured</li>
     *   <li>Otherwise, return the highest priority warehouse for the category</li>
     *   <li>Return empty if no warehouse is assigned to the category</li>
     * </ol>
     *
     * @param category the product category
     * @return optional warehouse ID for the category
     */
    private Optional<Long> getWarehouseForCategory(String category) {
        Optional<Long> defaultWarehouse = categoryWarehouseRepository
                .findDefaultWarehouseByCategory(category);

        if (defaultWarehouse.isPresent()) {
            return defaultWarehouse;
        }

        return categoryWarehouseRepository
                .findByCategoryOrderedByPriority(category)
                .stream()
                .findFirst()
                .map(CategoryWarehouse::getWarehouseId);
    }
}