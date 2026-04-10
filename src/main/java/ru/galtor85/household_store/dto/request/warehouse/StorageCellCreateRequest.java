package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

import static ru.galtor85.household_store.constants.TechnicalConstants.CELL_CODE_PATTERN;
import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_ACTIVE;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_NOTES_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_SECTION_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_RACK_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_SHELF_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_POSITION_LENGTH;

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

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    /**
     * Gets full cell path in format Section-Rack-Shelf-Position
     *
     * @return full path string
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getFullPath() {
        StringBuilder path = new StringBuilder();
        if (section != null) path.append(section);
        if (rack != null) path.append("-").append(rack);
        if (shelf != null) path.append("-").append(shelf);
        if (position != null) path.append("-").append(position);
        return path.toString();
    }

    /**
     * Checks if cell has full addressing (section + rack + shelf + position)
     *
     * @return true if all address parts are present
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasFullAddress() {
        return section != null && rack != null && shelf != null && position != null;
    }

    /**
     * Checks if cell is a pallet cell
     *
     * @return true if cell type is PALLET
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isPalletCell() {
        return CellType.PALLET.equals(cellType);
    }

    /**
     * Checks if cell requires special conditions (fridge, freezer, dangerous goods)
     *
     * @return true if special conditions required
     */
    @JsonIgnore
    @Schema(hidden = true)
    public boolean requiresSpecialConditions() {
        return cellType == CellType.FRIDGE ||
                cellType == CellType.FREEZER ||
                cellType == CellType.DANGEROUS;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCode() {
        return code != null && !code.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCellType() {
        return cellType != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSection() {
        return section != null && !section.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasRack() {
        return rack != null && !rack.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasShelf() {
        return shelf != null && !shelf.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPosition() {
        return position != null && !position.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMaxWeightKg() {
        return maxWeightKg != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMaxVolumeM3() {
        return maxVolumeM3 != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasIsActive() {
        return isActive != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isActiveTrue() {
        return Boolean.TRUE.equals(isActive);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCode() {
        return code != null ? code.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSection() {
        return section != null ? section.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedRack() {
        return rack != null ? rack.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedShelf() {
        return shelf != null ? shelf.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedPosition() {
        return position != null ? position.trim() : null;
    }
}