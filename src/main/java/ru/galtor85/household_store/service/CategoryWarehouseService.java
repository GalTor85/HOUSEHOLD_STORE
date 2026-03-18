package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.CategoryAlreadyAssignedException;
import ru.galtor85.household_store.advice.exception.CategoryNotFoundException;
import ru.galtor85.household_store.advice.exception.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.CategoryWarehouse;
import ru.galtor85.household_store.entity.Warehouse;
import ru.galtor85.household_store.mapper.CategoryWarehouseMapper;
import ru.galtor85.household_store.repository.CategoryWarehouseRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryWarehouseService {

    private final CategoryWarehouseRepository categoryWarehouseRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryWarehouseMapper mapper;
    private final MessageService messageService;

    /**
     * Привязка категории к складу
     */
    @Transactional
    public CategoryWarehouseDto assignCategoryToWarehouse(CategoryWarehouseRequest request,
                                                          Long createdBy,
                                                          Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем существование склада
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", request.getWarehouseId()));
                    return new WarehouseNotFoundException(request.getWarehouseId());
                });

        // Проверяем, не привязана ли уже категория
        if (categoryWarehouseRepository.existsByCategory(request.getCategory())) {
            log.warn(messageService.get("category.already.assigned", request.getCategory()));
            throw new CategoryAlreadyAssignedException(request.getCategory());
        }

        // Если это категория по умолчанию, сбрасываем флаг у других категорий?
        if (request.getIsDefault()) {
            // Можно сбросить предыдущую категорию по умолчанию для этого склада
            resetDefaultForWarehouse(request.getWarehouseId());
        }

        CategoryWarehouse categoryWarehouse = CategoryWarehouse.builder()
                .category(request.getCategory())
                .warehouseId(request.getWarehouseId())
                .isDefault(request.getIsDefault())
                .priority(request.getPriority())
                .build();

        CategoryWarehouse saved = categoryWarehouseRepository.save(categoryWarehouse);

        log.info(messageService.get("category.assigned.log",
                request.getCategory(), request.getWarehouseId(), createdBy));

        return mapper.toDto(saved, warehouse.getName());
    }

    /**
     * Обновление привязки категории
     */
    @Transactional
    public CategoryWarehouseDto updateCategoryAssignment(String category,
                                                         CategoryWarehouseRequest request,
                                                         Long updatedBy,
                                                         Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        CategoryWarehouse assignment = categoryWarehouseRepository.findByCategory(category)
                .orElseThrow(() -> {
                    log.error(messageService.get("category.not.found", category));
                    return new CategoryNotFoundException(category);
                });

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", request.getWarehouseId()));
                    return new WarehouseNotFoundException(request.getWarehouseId());
                });

        if (request.getIsDefault()) {
            resetDefaultForWarehouse(request.getWarehouseId());
        }

        assignment.setWarehouseId(request.getWarehouseId());
        assignment.setIsDefault(request.getIsDefault());
        assignment.setPriority(request.getPriority());

        CategoryWarehouse updated = categoryWarehouseRepository.save(assignment);

        log.info(messageService.get("category.updated.log", category, updatedBy));

        return mapper.toDto(updated, warehouse.getName());
    }

    /**
     * Удаление привязки категории
     */
    @Transactional
    public void deleteCategoryAssignment(String category, Long deletedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        CategoryWarehouse assignment = categoryWarehouseRepository.findByCategory(category)
                .orElseThrow(() -> {
                    log.error(messageService.get("category.not.found", category));
                    return new CategoryNotFoundException(category);
                });

        categoryWarehouseRepository.delete(assignment);

        log.info(messageService.get("category.deleted.log", category, deletedBy));
    }

    /**
     * Получение привязки по категории
     */
    @Transactional(readOnly = true)
    public CategoryWarehouseDto getCategoryAssignment(String category, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        CategoryWarehouse assignment = categoryWarehouseRepository.findByCategory(category)
                .orElseThrow(() -> {
                    log.error(messageService.get("category.not.found", category));
                    return new CategoryNotFoundException(category);
                });

        Warehouse warehouse = warehouseRepository.findById(assignment.getWarehouseId())
                .orElse(null);

        return mapper.toDto(assignment, warehouse != null ? warehouse.getName() : null);
    }

    /**
     * Получение всех привязок
     */
    @Transactional(readOnly = true)
    public List<CategoryWarehouseDto> getAllAssignments(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findAll();

        return assignments.stream()
                .map(assignment -> {
                    Warehouse warehouse = warehouseRepository.findById(assignment.getWarehouseId()).orElse(null);
                    return mapper.toDto(assignment, warehouse != null ? warehouse.getName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Получение склада для категории (используется в WarehouseResolver)
     */
    @Transactional(readOnly = true)
    public Long resolveWarehouseForCategory(String category) {
        // Сначала ищем дефолтный
        return categoryWarehouseRepository.findDefaultWarehouseByCategory(category)
                .orElseGet(() ->
                        categoryWarehouseRepository.findByCategoryOrderedByPriority(category)
                                .stream()
                                .findFirst()
                                .map(CategoryWarehouse::getWarehouseId)
                                .orElse(null)
                );
    }

    private void resetDefaultForWarehouse(Long warehouseId) {
        List<CategoryWarehouse> defaults = categoryWarehouseRepository.findByWarehouseId(warehouseId)
                .stream()
                .filter(CategoryWarehouse::getIsDefault)
                .collect(Collectors.toList());

        defaults.forEach(cw -> cw.setIsDefault(false));
        categoryWarehouseRepository.saveAll(defaults);
    }

    /**
     * Массовая привязка нескольких категорий к складу
     */
    @Transactional
    public List<CategoryWarehouseDto> bulkAssignCategories(BulkCategoryWarehouseRequest request,
                                                           Long createdBy,
                                                           Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем существование склада
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", request.getWarehouseId()));
                    return new WarehouseNotFoundException(request.getWarehouseId());
                });

        List<CategoryWarehouseDto> results = new ArrayList<>();
        List<String> alreadyAssigned = new ArrayList<>();
        List<String> newlyAssigned = new ArrayList<>();

        for (String category : request.getCategories()) {
            // Проверяем, не привязана ли уже категория
            if (categoryWarehouseRepository.findByCategory(category).isPresent()) {
                alreadyAssigned.add(category);
                continue;
            }

            // Если это категория по умолчанию, сбрасываем флаг у других категорий этого склада
            if (request.getIsDefault()) {
                resetDefaultForWarehouse(request.getWarehouseId());
            }

            CategoryWarehouse assignment = CategoryWarehouse.builder()
                    .category(category)
                    .warehouseId(request.getWarehouseId())
                    .isDefault(request.getIsDefault())
                    .priority(request.getPriority())
                    .build();

            CategoryWarehouse saved = categoryWarehouseRepository.save(assignment);
            results.add(mapper.toDto(saved, warehouse.getName()));
            newlyAssigned.add(category);
        }

        // Логируем результаты
        if (!newlyAssigned.isEmpty()) {
            log.info(messageService.get("category.bulk.assigned.log",
                    newlyAssigned.size(), request.getWarehouseId(), createdBy));
        }

        if (!alreadyAssigned.isEmpty()) {
            log.warn(messageService.get("category.bulk.already.assigned.log",
                    String.join(", ", alreadyAssigned)));
        }

        return results;
    }

    /**
     * Получение всех категорий, привязанных к складу
     */
    @Transactional(readOnly = true)
    public CategoryWarehouseListDto getCategoriesByWarehouse(Long warehouseId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.error(messageService.get("warehouse.not.found", warehouseId));
                    return new WarehouseNotFoundException(warehouseId);
                });

        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByWarehouseId(warehouseId);

        List<CategoryAssignmentDto> categoryDtos = assignments.stream()
                .map(a -> CategoryAssignmentDto.builder()
                        .id(a.getId())
                        .category(a.getCategory())
                        .isDefault(a.getIsDefault())
                        .priority(a.getPriority())
                        .build())
                .collect(Collectors.toList());

        return CategoryWarehouseListDto.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouse.getName())
                .totalCategories(assignments.size())
                .categories(categoryDtos)
                .build();
    }

    /**
     * Обновление приоритетов для нескольких категорий
     */
    @Transactional
    public void updateCategoriesPriority(List<String> categories, Integer priority,
                                         Long updatedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByCategoryIn(categories);

        for (CategoryWarehouse assignment : assignments) {
            assignment.setPriority(priority);
        }

        categoryWarehouseRepository.saveAll(assignments);

        log.info(messageService.get("category.bulk.priority.updated.log",
                categories.size(), priority, updatedBy));
    }

    /**
     * Удаление нескольких категорий
     */
    @Transactional
    public void deleteCategories(List<String> categories, Long deletedBy, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<CategoryWarehouse> assignments = categoryWarehouseRepository.findByCategoryIn(categories);
        categoryWarehouseRepository.deleteAll(assignments);

        log.info(messageService.get("category.bulk.deleted.log",
                assignments.size(), deletedBy));
    }
}