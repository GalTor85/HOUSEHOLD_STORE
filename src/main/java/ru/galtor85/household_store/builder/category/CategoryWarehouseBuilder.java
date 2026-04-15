package ru.galtor85.household_store.builder.category;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.warehouse.CategoryWarehouseRequest;
import ru.galtor85.household_store.dto.response.warehouse.CategoryAssignmentDto;
import ru.galtor85.household_store.dto.response.warehouse.CategoryWarehouseListDto;
import ru.galtor85.household_store.entity.warehouse.CategoryWarehouse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for category-warehouse assignment entities and DTOs.
 */
@Component
public class CategoryWarehouseBuilder {

    /**
     * Builds a CategoryWarehouse entity from the request.
     *
     * @param request the category warehouse request
     * @return CategoryWarehouse entity
     */
    public CategoryWarehouse buildFromRequest(CategoryWarehouseRequest request) {
        return CategoryWarehouse.builder()
                .category(request.getCategory())
                .warehouseId(request.getWarehouseId())
                .isDefault(request.getIsDefault())
                .priority(request.getPriority())
                .build();
    }

    /**
     * Builds a CategoryWarehouseListDto from warehouse data and assignments.
     *
     * @param warehouseId   warehouse ID
     * @param warehouseName warehouse name
     * @param assignments   list of category-warehouse assignments
     * @return CategoryWarehouseListDto
     */
    public CategoryWarehouseListDto buildCategoryWarehouseListDto(Long warehouseId,
                                                                  String warehouseName,
                                                                  List<CategoryWarehouse> assignments) {
        List<CategoryAssignmentDto> categoryDtos = assignments.stream()
                .map(this::buildCategoryAssignmentDto)
                .collect(Collectors.toList());

        return CategoryWarehouseListDto.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouseName)
                .totalCategories(assignments.size())
                .categories(categoryDtos)
                .build();
    }

    /**
     * Builds a CategoryAssignmentDto from a CategoryWarehouse entity.
     *
     * @param assignment the category-warehouse assignment entity
     * @return CategoryAssignmentDto
     */
    private CategoryAssignmentDto buildCategoryAssignmentDto(CategoryWarehouse assignment) {
        return CategoryAssignmentDto.builder()
                .id(assignment.getId())
                .category(assignment.getCategory())
                .isDefault(assignment.getIsDefault())
                .priority(assignment.getPriority())
                .build();
    }
}