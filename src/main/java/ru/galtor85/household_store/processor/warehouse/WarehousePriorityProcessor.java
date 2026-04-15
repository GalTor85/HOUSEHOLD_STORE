package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Processor for calculating warehouse priorities based on product assignments.
 *
 * <p>Determines the most suitable warehouse for a set of products by analyzing
 * direct warehouse assignments and category-based warehouse mappings.</p>
 *
 * <p>Priority weights:
 * <ul>
 *   <li>Direct product warehouse assignment: weight 100</li>
 *   <li>Category-based warehouse assignment: weight 50</li>
 *   <li>Suggestion counting: weight 1 per product</li>
 * </ul>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehousePriorityProcessor {

    private static final int DIRECT_ASSIGNMENT_WEIGHT = 100;
    private static final int CATEGORY_ASSIGNMENT_WEIGHT = 50;
    private static final int SUGGESTION_WEIGHT = 1;

    private final LogMessageService logMsg;

    /**
     * Collects warehouse priorities for a list of products.
     *
     * <p>Priority calculation rules:
     * <ol>
     *   <li>If product has direct warehouse assignment → adds 100 points</li>
     *   <li>If product has category with mapped warehouse → adds 50 points</li>
     *   <li>Products without any assignment are ignored</li>
     * </ol>
     *
     * <p>Multiple products assigned to the same warehouse will accumulate points.</p>
     *
     * @param products list of products to analyze
     * @param warehouseResolver function to resolve warehouse ID from product category
     * @return map of warehouse IDs to their accumulated priority scores
     */
    public Map<Long, Integer> collectPriorities(List<Product> products,
                                                Function<String, Optional<Long>> warehouseResolver) {
        Map<Long, Integer> warehousePriorities = new HashMap<>();

        for (Product product : products) {
            if (hasDirectWarehouseAssignment(product)) {
                addPriority(warehousePriorities, product.getWarehouseId(),
                        product);
            } else if (hasCategoryAssignment(product)) {
                resolveAndAddCategoryPriority(warehousePriorities, product, warehouseResolver);
            }
        }

        log.debug(logMsg.get("warehouse.priorities.collected",
                warehousePriorities.size(), products.size()));

        return warehousePriorities;
    }

    /**
     * Collects warehouse suggestions for a list of products.
     *
     * <p>Unlike priorities, suggestions use equal weight (1 point per product)
     * to show which warehouses are relevant for the given products.</p>
     *
     * @param products list of products to analyze
     * @param warehouseResolver function to resolve warehouse ID from product category
     * @return map of warehouse IDs to their suggestion counts
     */
    public Map<Long, Integer> collectSuggestions(List<Product> products,
                                                 Function<String, Optional<Long>> warehouseResolver) {
        Map<Long, Integer> suggestions = new HashMap<>();

        for (Product product : products) {
            if (hasDirectWarehouseAssignment(product)) {
                suggestions.merge(product.getWarehouseId(), SUGGESTION_WEIGHT, Integer::sum);
            } else if (hasCategoryAssignment(product)) {
                warehouseResolver.apply(product.getCategory())
                        .ifPresent(warehouseId -> suggestions.merge(warehouseId, SUGGESTION_WEIGHT, Integer::sum));
            }
        }

        log.debug(logMsg.get("warehouse.suggestions.collected",
                suggestions.size(), products.size()));

        return suggestions;
    }

    /**
     * Checks if a product has a direct warehouse assignment.
     *
     * @param product the product to check
     * @return true if product has warehouseId set
     */
    private boolean hasDirectWarehouseAssignment(Product product) {
        return product.getWarehouseId() != null;
    }

    /**
     * Checks if a product has a category that can be used for warehouse resolution.
     *
     * @param product the product to check
     * @return true if product has category set
     */
    private boolean hasCategoryAssignment(Product product) {
        return product.getCategory() != null;
    }

    /**
     * Adds priority points for a warehouse.
     *
     * @param priorities  map of warehouse priorities
     * @param warehouseId the warehouse ID
     * @param product     the product context (for logging)
     */
    private void addPriority(Map<Long, Integer> priorities,
                             Long warehouseId,
                             Product product) {
        priorities.merge(warehouseId, WarehousePriorityProcessor.DIRECT_ASSIGNMENT_WEIGHT, Integer::sum);
        log.debug(logMsg.get("warehouse.resolver.direct.assignment",
                product.getId(), warehouseId));
    }

    /**
     * Resolves warehouse from category and adds priority points.
     *
     * @param priorities map of warehouse priorities
     * @param product the product context
     * @param warehouseResolver function to resolve warehouse ID from category
     */
    private void resolveAndAddCategoryPriority(Map<Long, Integer> priorities,
                                               Product product,
                                               Function<String, Optional<Long>> warehouseResolver) {
        warehouseResolver.apply(product.getCategory()).ifPresent(warehouseId -> {
            priorities.merge(warehouseId, CATEGORY_ASSIGNMENT_WEIGHT, Integer::sum);
            log.debug(logMsg.get("warehouse.resolver.category.assignment",
                    product.getId(), product.getCategory(), warehouseId));
        });
    }
}