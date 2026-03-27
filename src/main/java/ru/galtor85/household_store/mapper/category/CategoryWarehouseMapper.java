package ru.galtor85.household_store.mapper.category;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.warehouse.CategoryWarehouseDto;
import ru.galtor85.household_store.entity.warehouse.CategoryWarehouse;

@Component
public class CategoryWarehouseMapper {

    public CategoryWarehouseDto toDto(CategoryWarehouse entity, String warehouseName) {
        if (entity == null) {
            return null;
        }

        return CategoryWarehouseDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .warehouseId(entity.getWarehouseId())
                .warehouseName(warehouseName)
                .isDefault(entity.getIsDefault())
                .priority(entity.getPriority())
                .build();
    }
}