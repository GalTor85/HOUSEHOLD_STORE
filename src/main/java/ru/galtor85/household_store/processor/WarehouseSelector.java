package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.entity.SalesOrder;
import ru.galtor85.household_store.service.MessageService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseSelector {

    private final WarehouseConfig warehouseConfig;
    private final MessageService messageService;

    public Long selectByPriority(Map<Long, Integer> warehousePriorities, Long orderId) {
        if (warehousePriorities.isEmpty()) {
            Long defaultWarehouse = warehouseConfig.getDefaultWarehouseId();
            log.info(messageService.get("warehouse.resolver.using.default",
                    defaultWarehouse, orderId));
            return defaultWarehouse;
        }

        Long selectedWarehouse = warehousePriorities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(warehouseConfig.getDefaultWarehouseId());

        log.info(messageService.get("warehouse.resolver.selected",
                selectedWarehouse, orderId,
                warehousePriorities.getOrDefault(selectedWarehouse, 0)));

        return selectedWarehouse;
    }

    public Long selectForced(Long forcedWarehouseId, SalesOrder salesOrder) {
        if (forcedWarehouseId != null) {
            log.info(messageService.get("warehouse.resolver.using.forced",
                    forcedWarehouseId, salesOrder.getId()));
            return forcedWarehouseId;
        }
        return null;
    }

    public List<WarehouseSuggestion> getSortedSuggestions(Map<Long, Integer> suggestions) {
        return suggestions.entrySet().stream()
                .map(e -> new WarehouseSuggestion(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(WarehouseSuggestion::getProductCount).reversed())
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class WarehouseSuggestion {
        private Long warehouseId;
        private int productCount;
    }
}