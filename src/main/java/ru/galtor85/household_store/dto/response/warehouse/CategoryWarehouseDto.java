package ru.galtor85.household_store.dto.response.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Category warehouse DTO", title = "Category Warehouse")
public class CategoryWarehouseDto {

    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    @Schema(description = "Category name", example = "Electronics")
    private String category;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Is default for this category", example = "true")
    private Boolean isDefault;

    @Schema(description = "Priority", example = "1")
    private Integer priority;
}