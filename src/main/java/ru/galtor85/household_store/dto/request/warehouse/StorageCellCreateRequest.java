package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for creating a storage cell.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage cell create request", title = "Storage Cell Create Request")
public class StorageCellCreateRequest {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotBlank(message = "{cell.validation.code.empty}")
    @Pattern(regexp = CELL_CODE_PATTERN, message = "{cell.validation.code.pattern}")
    @Schema(description = "Cell code (format: SECTION-RACK-SHELF-POSITION)",
            example = "A-01-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull(message = "{cell.validation.type.empty}")
    @Schema(description = "Cell type", example = "STANDARD", requiredMode = Schema.RequiredMode.REQUIRED)
    private CellType cellType;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Size(max = MAX_SECTION_LENGTH, message = "{cell.validation.section.max}")
    @Schema(description = "Section (A, B, C...)", example = "A")
    private String section;

    @Size(max = MAX_RACK_LENGTH, message = "{cell.validation.rack.max}")
    @Schema(description = "Rack number", example = "01")
    private String rack;

    @Size(max = MAX_SHELF_LENGTH, message = "{cell.validation.shelf.max}")
    @Schema(description = "Shelf number", example = "01")
    private String shelf;

    @Size(max = MAX_POSITION_LENGTH, message = "{cell.validation.position.max}")
    @Schema(description = "Position on shelf", example = "01")
    private String position;

    @Positive(message = "{cell.validation.max.weight.positive}")
    @Schema(description = "Maximum weight capacity in kg", example = "100.0")
    private Double maxWeightKg;

    @Positive(message = "{cell.validation.max.volume.positive}")
    @Schema(description = "Maximum volume capacity in m³", example = "0.5")
    private Double maxVolumeM3;

    @Schema(description = "Cell is active", example = "true", defaultValue = "true")
    private Boolean isActive = DEFAULT_ACTIVE;

    @Size(max = MAX_NOTES_LENGTH, message = "{cell.validation.notes.max}")
    @Schema(description = "Notes about the cell", example = "Near the entrance, suitable for fragile items")
    private String notes;
}