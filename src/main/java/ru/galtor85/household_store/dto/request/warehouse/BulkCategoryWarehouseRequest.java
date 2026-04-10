package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_PRIORITY;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_IS_DEFAULT;

/**
 * Request DTO for bulk category warehouse assignment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk category warehouse assignment request", title = "Bulk Category Warehouse Request")
public class BulkCategoryWarehouseRequest {

    @NotNull(message = "{category.validation.warehouse.id.empty}")
    @Schema(description = "Warehouse ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    @NotNull(message = "{category.validation.categories.empty}")
    @Size(min = 1, message = "{category.validation.categories.min}")
    @Schema(description = "List of categories to assign", example = "[\"Electronics\", \"Clothing\", \"Books\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> categories;

    @Schema(description = "Is default for these categories", example = "true")
    private Boolean isDefault = DEFAULT_IS_DEFAULT;

    @Schema(description = "Priority for all assignments", example = "1")
    private Integer priority = DEFAULT_PRIORITY;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWarehouseId() {
        return warehouseId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasIsDefault() {
        return isDefault != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPriority() {
        return priority != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDefaultTrue() {
        return Boolean.TRUE.equals(isDefault);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getCategoriesCount() {
        return categories != null ? categories.size() : 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public List<String> getNormalizedCategories() {
        if (categories == null) return null;
        return categories.stream()
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .toList();
    }
}