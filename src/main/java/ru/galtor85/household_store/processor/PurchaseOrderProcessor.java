package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.PurchaseOrderBuilder;
import ru.galtor85.household_store.dto.CategoryWarehouseDto;
import ru.galtor85.household_store.dto.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.PurchaseOrderRepository;
import ru.galtor85.household_store.service.CategoryWarehouseService;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderProcessor {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderBuilder builder;
    private final CategoryWarehouseService categoryWarehouseService;  // ← ДОБАВЛЕНО
    private final MessageService messageService;

    /**
     * Создает заказ на закупку
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderCreateRequest request,
                                             Supplier supplier,
                                             List<Product> products,
                                             List<BigDecimal> prices,
                                             Long managerId) {

        log.info(messageService.get("purchase.order.processor.start",
                request.getSupplierId(), managerId));

        // 1. Определяем склад по категориям товаров
        Long warehouseId = determineWarehouseId(products);

        // 2. Создаем заказ через билдер
        PurchaseOrder order = builder.buildOrder(request, managerId);

        // 3. Устанавливаем склад (если определен)
        if (warehouseId != null) {
            order.setWarehouseLocation(String.valueOf(warehouseId));
            log.debug(messageService.get("purchase.order.processor.warehouse.set", warehouseId));
        }

        // 4. Создаем позиции через билдер
        List<PurchaseOrderItem> items = builder.buildOrderItemsFromCreate(
                order,
                request.getItems(),
                products,
                prices
        );

        // 5. Рассчитываем сумму
        BigDecimal totalAmount = builder.calculateTotalAmount(items);

        // 6. Устанавливаем позиции и суммы
        order.setItems(items);
        order.setSubtotal(totalAmount);
        order.setTotalAmount(totalAmount);

        // 7. Сохраняем
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(messageService.get("purchase.order.processor.complete",
                savedOrder.getOrderNumber(),
                savedOrder.getId(),
                items.size(),
                totalAmount));

        return savedOrder;
    }

    /**
     * Определяет склад по категориям товаров
     */
    private Long determineWarehouseId(List<Product> products) {
        if (products == null || products.isEmpty()) {
            log.debug(messageService.get("purchase.order.processor.no.products"));
            return null;
        }

        // Собираем уникальные категории товаров
        List<String> categories = products.stream()
                .map(Product::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            log.debug(messageService.get("purchase.order.processor.no.categories"));
            return null;
        }

        // Для каждой категории получаем склад и приоритет
        Map<Long, Integer> warehousePriorities = new HashMap<>();

        for (String category : categories) {
            Long warehouseId = categoryWarehouseService.resolveWarehouseForCategory(category);
            if (warehouseId != null) {
                // Получаем приоритет склада для этой категории
                int priority = getWarehousePriority(category, warehouseId);
                warehousePriorities.merge(warehouseId, priority, Integer::sum);
                log.debug(messageService.get("purchase.order.processor.category.warehouse",
                        category, warehouseId, priority));
            }
        }

        if (warehousePriorities.isEmpty()) {
            log.debug(messageService.get("purchase.order.processor.no.warehouse.for.categories"));
            return null;
        }

        // Выбираем склад с наивысшим приоритетом
        return warehousePriorities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Получает приоритет склада для категории
     */
    private int getWarehousePriority(String category, Long warehouseId) {
        try {
            CategoryWarehouseDto assignment = categoryWarehouseService.getCategoryAssignment(category);
            if (assignment != null && assignment.getWarehouseId().equals(warehouseId)) {
                return assignment.getPriority() != null ? assignment.getPriority() : 0;
            }
        } catch (Exception e) {
            log.debug(messageService.get("purchase.order.processor.priority.error", category, e.getMessage()));
        }
        return 0;
    }
}