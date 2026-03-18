package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.entity.CategoryWarehouse;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.repository.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseResolver {

    private final WarehouseConfig warehouseConfig;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final MessageService messageService;

    /**
     * Определение склада с приоритетами:
     * 1. Прямое назначение в продукте (warehouseId)
     * 2. Склад из категории продукта
     * 3. Склад по умолчанию из настроек
     */
    public Long resolveWarehouseId(Order order) {
        if (order.getItems().isEmpty()) {
            log.warn(messageService.get("warehouse.resolver.no.items", order.getId()));
            return warehouseConfig.getDefaultWarehouseId();
        }

        // Получаем все продукты из заказа
        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);

        // Собираем все возможные склады с весами
        Map<Long, Integer> warehousePriorities = new HashMap<>();

        for (Product product : products) {
            // Приоритет 1: Прямое назначение (вес 100)
            if (product.getWarehouseId() != null) {
                warehousePriorities.merge(product.getWarehouseId(), 100, Integer::sum);
                log.debug("Product {} has direct warehouse: {}", product.getId(), product.getWarehouseId());
            }

            // Приоритет 2: Склад из категории (вес 50) - ИСПОЛЬЗУЕМ СУЩЕСТВУЮЩИЙ МЕТОД
            else if (product.getCategory() != null) {
                getWarehouseForCategory(product.getCategory()).ifPresent(warehouseId -> {
                    warehousePriorities.merge(warehouseId, 50, Integer::sum);
                    log.debug("Product {} category {} -> warehouse: {}",
                            product.getId(), product.getCategory(), warehouseId);
                });
            }
        }

        // Выбираем склад с наибольшим весом
        Long selectedWarehouse = warehousePriorities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(warehouseConfig.getDefaultWarehouseId());

        log.info(messageService.get("warehouse.resolver.selected",
                selectedWarehouse, order.getId(),
                warehousePriorities.getOrDefault(selectedWarehouse, 0)));

        return selectedWarehouse;
    }

    /**
     * Получение склада для категории с учетом приоритетов (СУЩЕСТВУЮЩИЙ МЕТОД)
     */
    private Optional<Long> getWarehouseForCategory(String category) {
        // Сначала ищем склад по умолчанию для категории
        Optional<Long> defaultWarehouse = categoryWarehouseRepository
                .findDefaultWarehouseByCategory(category);

        if (defaultWarehouse.isPresent()) {
            return defaultWarehouse;
        }

        // Иначе берем первый по приоритету
        List<CategoryWarehouse> warehouses = categoryWarehouseRepository
                .findByCategoryOrderedByPriority(category);

        return warehouses.stream()
                .findFirst()
                .map(CategoryWarehouse::getWarehouseId);
    }

    /**
     * Определение склада с возможностью прямого указания
     */
    public Long resolveWarehouseId(Order order, Long forcedWarehouseId) {
        if (forcedWarehouseId != null) {
            // Проверяем, существует ли такой склад
            if (!warehouseRepository.existsById(forcedWarehouseId)) {
                log.warn("Forced warehouse {} does not exist, using auto resolution", forcedWarehouseId);
                return resolveWarehouseId(order);
            }
            log.info("Using forced warehouse: {} for order {}", forcedWarehouseId, order.getId());
            return forcedWarehouseId;
        }
        return resolveWarehouseId(order);
    }

    /**
     * Получение списка возможных складов для заказа
     */
    public List<WarehouseSuggestion> getWarehouseSuggestions(Order order) {
        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Integer> suggestions = new HashMap<>();

        for (Product product : products) {
            if (product.getWarehouseId() != null) {
                suggestions.merge(product.getWarehouseId(), 1, Integer::sum);
            } else if (product.getCategory() != null) {
                getWarehouseForCategory(product.getCategory()).ifPresent(warehouseId -> {
                    suggestions.merge(warehouseId, 1, Integer::sum);
                });
            }
        }

        return suggestions.entrySet().stream()
                .map(e -> new WarehouseSuggestion(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(WarehouseSuggestion::getProductCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Внутренний класс для предложений складов
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class WarehouseSuggestion {
        private Long warehouseId;
        private int productCount;
    }
}