package ru.galtor85.household_store.builder;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.BulkCategoryWarehouseRequest;
import ru.galtor85.household_store.dto.CategoryAssignmentDto;
import ru.galtor85.household_store.dto.CategoryWarehouseListDto;
import ru.galtor85.household_store.dto.CategoryWarehouseRequest;
import ru.galtor85.household_store.entity.CategoryWarehouse;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryWarehouseBuilder {

    public CategoryWarehouse buildFromRequest(CategoryWarehouseRequest request) {
        return CategoryWarehouse.builder()
                .category(request.getCategory())
                .warehouseId(request.getWarehouseId())
                .isDefault(request.getIsDefault())
                .priority(request.getPriority())
                .build();
    }

    public List<CategoryWarehouse> buildFromBulkRequest(BulkCategoryWarehouseRequest request) {
        return request.getCategories().stream()
                .map(category -> CategoryWarehouse.builder()
                        .category(category)
                        .warehouseId(request.getWarehouseId())
                        .isDefault(request.getIsDefault())
                        .priority(request.getPriority())
                        .build())
                .collect(Collectors.toList());
    }

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

    private CategoryAssignmentDto buildCategoryAssignmentDto(CategoryWarehouse assignment) {
        return CategoryAssignmentDto.builder()
                .id(assignment.getId())
                .category(assignment.getCategory())
                .isDefault(assignment.getIsDefault())
                .priority(assignment.getPriority())
                .build();
    }
}