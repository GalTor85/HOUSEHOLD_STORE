package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.service.MessageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehousePriorityProcessor {

    private final MessageService messageService;

    public Map<Long, Integer> collectPriorities(List<Product> products,
                                                Function<String, java.util.Optional<Long>> warehouseResolver) {

        Map<Long, Integer> warehousePriorities = new HashMap<>();

        for (Product product : products) {
            // Приоритет 1: Прямое назначение (вес 100)
            if (product.getWarehouseId() != null) {
                warehousePriorities.merge(product.getWarehouseId(), 100, Integer::sum);
                log.debug(messageService.get("warehouse.resolver.direct.assignment",
                        product.getId(), product.getWarehouseId()));
            }
            // Приоритет 2: Склад из категории (вес 50)
            else if (product.getCategory() != null) {
                warehouseResolver.apply(product.getCategory()).ifPresent(warehouseId -> {
                    warehousePriorities.merge(warehouseId, 50, Integer::sum);
                    log.debug(messageService.get("warehouse.resolver.category.assignment",
                            product.getId(), product.getCategory(), warehouseId));
                });
            }
        }

        return warehousePriorities;
    }

    public Map<Long, Integer> collectSuggestions(List<Product> products,
                                                 Function<String, java.util.Optional<Long>> warehouseResolver) {

        Map<Long, Integer> suggestions = new HashMap<>();

        for (Product product : products) {
            if (product.getWarehouseId() != null) {
                suggestions.merge(product.getWarehouseId(), 1, Integer::sum);
            } else if (product.getCategory() != null) {
                warehouseResolver.apply(product.getCategory()).ifPresent(warehouseId -> {
                    suggestions.merge(warehouseId, 1, Integer::sum);
                });
            }
        }

        return suggestions;
    }
}