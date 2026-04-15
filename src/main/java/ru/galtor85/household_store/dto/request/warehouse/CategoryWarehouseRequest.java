package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for category warehouse assignment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category warehouse assignment request", title = "Category Warehouse Request")
public class CategoryWarehouseRequest {

    @NotBlank(message = "{category.validation.name.empty}")
    @Size(max = MAX_CATEGORY_NAME_LENGTH, message = "{category.validation.name.max}")
    @Schema(description = "Category name", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @NotNull(message = "{category.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    @Schema(description = "Is default warehouse for this category", example = "true")
    private Boolean isDefault = DEFAULT_IS_DEFAULT;

    @Schema(description = "Priority (higher priority = more important)", example = "1")
    private Integer priority = DEFAULT_PRIORITY;
}