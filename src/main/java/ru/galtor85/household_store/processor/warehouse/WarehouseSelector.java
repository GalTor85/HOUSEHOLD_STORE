package ru.galtor85.household_store.processor.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Processor for selecting the optimal warehouse based on priorities.
 *
 * <p>Provides methods to select a warehouse using different strategies:
 * <ul>
 *   <li>Priority-based selection - chooses warehouse with highest accumulated weight</li>
 *   <li>Forced selection - uses explicitly specified warehouse</li>
 *   <li>Suggestion ranking - returns sorted list of candidate warehouses</li>
 * </ul>
 *
 * <p>When no suitable warehouse is found, falls back to the default warehouse
 * configured in {@link WarehouseConfig}.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSelector {

    private final WarehouseConfig warehouseConfig;
    private final LogMessageService logMsg;

    /**
     * Selects the warehouse with the highest priority score.
     *
     * <p>Analyzes the priority map and returns the warehouse ID with the maximum
     * accumulated weight. If the priority map is empty or no clear winner exists,
     * falls back to the default warehouse.</p>
     *
     * @param warehousePriorities map of warehouse IDs to their priority scores
     * @param orderId the order ID for logging context
     * @return selected warehouse ID
     */
    public Long selectByPriority(Map<Long, Integer> warehousePriorities, Long orderId) {
        if (warehousePriorities.isEmpty()) {
            Long defaultWarehouse = warehouseConfig.getDefaultWarehouseId();
            log.info(logMsg.get("warehouse.resolver.using.default",
                    defaultWarehouse, orderId));
            return defaultWarehouse;
        }

        Long selectedWarehouse = findWarehouseWithMaxPriority(warehousePriorities);

        log.info(logMsg.get("warehouse.resolver.selected",
                selectedWarehouse, orderId,
                warehousePriorities.getOrDefault(selectedWarehouse, 0)));

        return selectedWarehouse;
    }

    /**
     * Selects a forced warehouse when explicitly specified.
     *
     * <p>Use this method when the warehouse is predetermined by business rules
     * or user input, bypassing the priority calculation.</p>
     *
     * @param forcedWarehouseId the explicitly specified warehouse ID (maybe null)
     * @param salesOrder the sales order for logging context
     * @return the forced warehouse ID, or null if not specified
     */
    public Long selectForced(Long forcedWarehouseId, SalesOrder salesOrder) {
        if (forcedWarehouseId != null) {
            log.info(logMsg.get("warehouse.resolver.using.forced",
                    forcedWarehouseId, salesOrder.getId()));
            return forcedWarehouseId;
        }
        return null;
    }

    /**
     * Returns warehouse suggestions sorted by product count descending.
     *
     * <p>Useful for displaying a ranked list of warehouse options to the user,
     * where warehouses with more matching products appear first.</p>
     *
     * @param suggestions map of warehouse IDs to their suggestion counts
     * @return list of warehouse suggestions sorted by product count (highest first)
     */
    public List<WarehouseSuggestion> getSortedSuggestions(Map<Long, Integer> suggestions) {
        return suggestions.entrySet().stream()
                .map(this::toWarehouseSuggestion)
                .sorted(Comparator.comparing(WarehouseSuggestion::productCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Finds the warehouse with the maximum priority score.
     *
     * @param priorities map of warehouse priorities
     * @return warehouse ID with the highest score, or default if none found
     */
    private Long findWarehouseWithMaxPriority(Map<Long, Integer> priorities) {
        return priorities.entrySet().stream()
                .max(Entry.comparingByValue())
                .map(Entry::getKey)
                .orElseGet(warehouseConfig::getDefaultWarehouseId);
    }

    /**
     * Converts a map entry to a WarehouseSuggestion.
     *
     * @param entry map entry containing warehouse ID and product count
     * @return WarehouseSuggestion object
     */
    private WarehouseSuggestion toWarehouseSuggestion(Entry<Long, Integer> entry) {
        return new WarehouseSuggestion(entry.getKey(), entry.getValue());
    }

    /**
     * Represents a warehouse suggestion with its relevance score.
     *
     * @param warehouseId the warehouse identifier
     * @param productCount number of products that suggest this warehouse
     */
    public record WarehouseSuggestion(Long warehouseId, int productCount) {}
}