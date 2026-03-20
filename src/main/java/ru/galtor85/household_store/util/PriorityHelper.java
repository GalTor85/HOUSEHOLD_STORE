package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.CategoryWarehouse;
import ru.galtor85.household_store.repository.CategoryWarehouseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PriorityHelper {

    private final CategoryWarehouseRepository categoryWarehouseRepository;

    /**
     * Сброс флага isDefault для всех категорий склада
     */
    public void resetDefaultForWarehouse(Long warehouseId) {
        List<CategoryWarehouse> defaults = categoryWarehouseRepository.findByWarehouseId(warehouseId)
                .stream()
                .filter(CategoryWarehouse::getIsDefault)
                .collect(Collectors.toList());

        defaults.forEach(cw -> cw.setIsDefault(false));
        categoryWarehouseRepository.saveAll(defaults);
    }

    /**
     * Получение следующего доступного приоритета
     */
    public Integer getNextPriority(Long warehouseId) {
        return categoryWarehouseRepository.findByWarehouseId(warehouseId)
                .stream()
                .mapToInt(CategoryWarehouse::getPriority)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Пересчет приоритетов после удаления
     */
    public void reorderPriorities(Long warehouseId) {
        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByWarehouseId(warehouseId);
        assignments.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

        for (int i = 0; i < assignments.size(); i++) {
            assignments.get(i).setPriority(i + 1);
        }

        categoryWarehouseRepository.saveAll(assignments);
    }
}