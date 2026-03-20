package ru.galtor85.household_store.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.CategoryWarehouse;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.processor.WarehousePriorityProcessor;
import ru.galtor85.household_store.processor.WarehouseSelector;
import ru.galtor85.household_store.repository.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.validator.WarehouseValidator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseResolver {

    private final ProductRepository productRepository;
    private final CategoryWarehouseRepository categoryWarehouseRepository;

    // Валидаторы
    private final WarehouseValidator validator;

    // Процессоры
    private final WarehousePriorityProcessor priorityProcessor;
    private final WarehouseSelector selector;

    /**
     * Определение склада с приоритетами:
     * 1. Прямое назначение в продукте (warehouseId)
     * 2. Склад из категории продукта
     * 3. Склад по умолчанию из настроек
     */
    public Long resolveWarehouseId(Order order) {
        validator.validateOrderHasItems(order);

        // Получаем все продукты из заказа
        List<Product> products = getProductsFromOrder(order);

        // Собираем приоритеты
        Map<Long, Integer> warehousePriorities = priorityProcessor.collectPriorities(
                products, this::getWarehouseForCategory);

        // Выбираем склад
        return selector.selectByPriority(warehousePriorities, order.getId());
    }

    /**
     * Определение склада с возможностью прямого указания
     */
    public Long resolveWarehouseId(Order order, Long forcedWarehouseId) {
        Long forced = selector.selectForced(forcedWarehouseId, order);
        if (forced != null) {
            validator.validateWarehouseExists(forced);
            return forced;
        }

        return resolveWarehouseId(order);
    }

    /**
     * Получение списка возможных складов для заказа
     */
    public List<WarehouseSelector.WarehouseSuggestion> getWarehouseSuggestions(Order order) {
        List<Product> products = getProductsFromOrder(order);

        Map<Long, Integer> suggestions = priorityProcessor.collectSuggestions(
                products, this::getWarehouseForCategory);

        return selector.getSortedSuggestions(suggestions);
    }

    /**
     * Получение склада для категории с учетом приоритетов
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

    private List<Product> getProductsFromOrder(Order order) {
        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());

        return productRepository.findAllById(productIds);
    }
}