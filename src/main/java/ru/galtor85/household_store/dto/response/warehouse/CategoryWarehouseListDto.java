package ru.galtor85.household_store.dto.response.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Category warehouse list DTO", title = "Category Warehouse List")
public class CategoryWarehouseListDto {

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Number of categories assigned", example = "5")
    private int totalCategories;

    @Schema(description = "List of assigned categories")
    private List<CategoryAssignmentDto> categories;
}

