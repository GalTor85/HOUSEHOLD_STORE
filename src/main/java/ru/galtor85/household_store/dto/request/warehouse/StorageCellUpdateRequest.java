package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for updating a storage cell.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage cell update request", title = "Storage Cell Update Request")
public class StorageCellUpdateRequest {

    @Schema(description = "Cell type", example = "STANDARD")
    private CellType cellType;

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

    @Schema(description = "Cell is active", example = "true")
    private Boolean isActive;

    @Size(max = MAX_NOTES_LENGTH, message = "{cell.validation.notes.max}")
    @Schema(description = "Notes about the cell", example = "Near the entrance")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

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
    public boolean hasAnyUpdate() {
        return cellType != null ||
                hasSection() ||
                hasRack() ||
                hasShelf() ||
                hasPosition() ||
                hasMaxWeightKg() ||
                hasMaxVolumeM3() ||
                hasIsActive() ||
                hasNotes();
    }
}