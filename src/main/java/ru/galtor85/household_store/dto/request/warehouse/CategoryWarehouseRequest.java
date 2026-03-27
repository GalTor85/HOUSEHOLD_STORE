package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category warehouse assignment request", title = "Category Warehouse Request")
public class CategoryWarehouseRequest {

    @NotBlank(message = "{category.validation.name.empty}")
    @Schema(description = "Category name", example = "Electronics", required = true)
    private String category;

    @NotNull(message = "{category.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID", example = "1", required = true)
    private Long warehouseId;

    @Schema(description = "Is default warehouse for this category", example = "true")
    private Boolean isDefault = false;

    @Schema(description = "Priority (higher priority = more important)", example = "1")
    private Integer priority = 0;
}