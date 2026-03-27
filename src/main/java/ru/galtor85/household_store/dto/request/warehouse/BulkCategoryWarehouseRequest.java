package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk category warehouse assignment request", title = "Bulk Category Warehouse Request")
public class BulkCategoryWarehouseRequest {

    @NotNull(message = "{category.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID", example = "1", required = true)
    private Long warehouseId;

    @NotNull(message = "{category.validation.categories.empty}")
    @Size(min = 1, message = "{category.validation.categories.min}")
    @Schema(description = "List of categories to assign", example = "[\"Electronics\", \"Clothing\", \"Books\"]", required = true)
    private List<String> categories;

    @Schema(description = "Is default for these categories", example = "true")
    private Boolean isDefault = false;

    @Schema(description = "Priority for all assignments", example = "1")
    private Integer priority = 0;
}