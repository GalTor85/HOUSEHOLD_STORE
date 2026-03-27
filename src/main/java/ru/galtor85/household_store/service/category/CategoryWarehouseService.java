package ru.galtor85.household_store.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.category.CategoryWarehouseBuilder;
import ru.galtor85.household_store.dto.request.warehouse.BulkCategoryWarehouseRequest;
import ru.galtor85.household_store.dto.request.warehouse.CategoryWarehouseRequest;
import ru.galtor85.household_store.dto.response.warehouse.CategoryWarehouseDto;
import ru.galtor85.household_store.dto.response.warehouse.CategoryWarehouseListDto;
import ru.galtor85.household_store.entity.warehouse.CategoryWarehouse;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.mapper.category.CategoryWarehouseMapper;
import ru.galtor85.household_store.processor.bulk.BulkCategoryProcessor;
import ru.galtor85.household_store.repository.category.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.priority.PriorityHelper;
import ru.galtor85.household_store.validator.category.CategoryWarehouseValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryWarehouseService {

    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final CategoryWarehouseMapper mapper;
    private final CategoryWarehouseBuilder builder;
    private final CategoryWarehouseValidator validator;
    private final BulkCategoryProcessor bulkProcessor;
    private final PriorityHelper priorityHelper;
    private final MessageService messageService;
    private final WarehouseRepository warehouseRepository;


    // ========== ОСНОВНЫЕ ОПЕРАЦИИ ==========

    @Transactional
    public CategoryWarehouseDto assignCategoryToWarehouse(CategoryWarehouseRequest request,
                                                          Long createdBy) {
        // Валидация
        Warehouse warehouse = validator.validateWarehouseExists(request.getWarehouseId());
        validator.validateCategoryNotAssigned(request.getCategory());

        // Бизнес-логика
        if (request.getIsDefault()) {
            priorityHelper.resetDefaultForWarehouse(request.getWarehouseId());
        }

        // Создание через билдер
        CategoryWarehouse categoryWarehouse = builder.buildFromRequest(request);
        CategoryWarehouse saved = categoryWarehouseRepository.save(categoryWarehouse);

        log.info(messageService.get("category.assigned.log",
                request.getCategory(), request.getWarehouseId(), createdBy));

        return mapper.toDto(saved, warehouse.getName());
    }

    @Transactional
    public CategoryWarehouseDto updateCategoryAssignment(String category,
                                                         CategoryWarehouseRequest request,
                                                         Long updatedBy) {
        // Валидация
        CategoryWarehouse assignment = validator.validateCategoryExists(category);
        Warehouse warehouse = validator.validateWarehouseExists(request.getWarehouseId());

        // Бизнес-логика
        if (request.getIsDefault()) {
            priorityHelper.resetDefaultForWarehouse(request.getWarehouseId());
        }

        // Обновление
        assignment.setWarehouseId(request.getWarehouseId());
        assignment.setIsDefault(request.getIsDefault());
        assignment.setPriority(request.getPriority());

        CategoryWarehouse updated = categoryWarehouseRepository.save(assignment);

        log.info(messageService.get("category.updated.log", category, updatedBy));

        return mapper.toDto(updated, warehouse.getName());
    }

    @Transactional
    public void deleteCategoryAssignment(String category, Long deletedBy) {
        CategoryWarehouse assignment = validator.validateCategoryExists(category);
        Long warehouseId = assignment.getWarehouseId();

        categoryWarehouseRepository.delete(assignment);

        // Пересчитываем приоритеты после удаления
        priorityHelper.reorderPriorities(warehouseId);

        log.info(messageService.get("category.deleted.log", category, deletedBy));
    }

    // ========== ПОЛУЧЕНИЕ ДАННЫХ ==========

    @Transactional(readOnly = true)
    public CategoryWarehouseDto getCategoryAssignment(String category) {
        CategoryWarehouse assignment = validator.validateCategoryExists(category);
        Warehouse warehouse = warehouseRepository.findById(assignment.getWarehouseId()).orElse(null);

        return mapper.toDto(assignment, warehouse != null ? warehouse.getName() : null);
    }

    @Transactional(readOnly = true)
    public List<CategoryWarehouseDto> getAllAssignments() {
        return categoryWarehouseRepository.findAll().stream()
                .map(assignment -> {
                    Warehouse warehouse = warehouseRepository.findById(assignment.getWarehouseId()).orElse(null);
                    return mapper.toDto(assignment, warehouse != null ? warehouse.getName() : null);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryWarehouseListDto getCategoriesByWarehouse(Long warehouseId) {
        Warehouse warehouse = validator.validateWarehouseExists(warehouseId);
        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByWarehouseId(warehouseId);

        return builder.buildCategoryWarehouseListDto(warehouseId, warehouse.getName(), assignments);
    }

    // ========== МАССОВЫЕ ОПЕРАЦИИ ==========

    @Transactional
    public List<CategoryWarehouseDto> bulkAssignCategories(BulkCategoryWarehouseRequest request,
                                                           Long createdBy) {
        validator.validateCategoriesForBulkAssign(request);
        Warehouse warehouse = validator.validateWarehouseExists(request.getWarehouseId());

        if (request.getIsDefault()) {
            priorityHelper.resetDefaultForWarehouse(request.getWarehouseId());
        }

        BulkCategoryProcessor.BulkAssignmentResult result = bulkProcessor.processBulkAssign(
                request, warehouse, createdBy);

        return result.getResults();
    }

    @Transactional
    public void updateCategoriesPriority(List<String> categories, Integer priority,
                                         Long updatedBy) {
        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByCategoryIn(categories);

        assignments.forEach(a -> a.setPriority(priority));
        categoryWarehouseRepository.saveAll(assignments);

        log.info(messageService.get("category.bulk.priority.updated.log",
                categories.size(), priority, updatedBy));
    }

    @Transactional
    public void deleteCategories(List<String> categories, Long deletedBy) {
        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByCategoryIn(categories);

        if (!assignments.isEmpty()) {
            Long warehouseId = assignments.get(0).getWarehouseId();
            categoryWarehouseRepository.deleteAll(assignments);
            priorityHelper.reorderPriorities(warehouseId);
        }

        log.info(messageService.get("category.bulk.deleted.log",
                assignments.size(), deletedBy));
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    @Transactional(readOnly = true)
    public Long resolveWarehouseForCategory(String category) {
        return categoryWarehouseRepository.findDefaultWarehouseByCategory(category)
                .orElseGet(() ->
                        categoryWarehouseRepository.findByCategoryOrderedByPriority(category)
                                .stream()
                                .findFirst()
                                .map(CategoryWarehouse::getWarehouseId)
                                .orElse(null)
                );
    }
}