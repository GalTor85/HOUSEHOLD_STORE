package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Storage cell update request")
public class StorageCellUpdateRequest {

    @Size(max = MAX_CELL_CODE_LENGTH, message = "{cell.validation.code.max}")
    @Schema(description = "Cell code", example = "A-01-01-01")
    private String code;

    @Size(max = MAX_SECTION_LENGTH, message = "{cell.validation.section.max}")
    @Schema(description = "Section", example = "A")
    private String section;

    @Size(max = MAX_RACK_LENGTH, message = "{cell.validation.rack.max}")
    @Schema(description = "Rack", example = "01")
    private String rack;

    @Size(max = MAX_SHELF_LENGTH, message = "{cell.validation.shelf.max}")
    @Schema(description = "Shelf", example = "01")
    private String shelf;

    @Size(max = MAX_POSITION_LENGTH, message = "{cell.validation.position.max}")
    @Schema(description = "Position", example = "01")
    private String position;

    @Schema(description = "Cell type", example = "STANDARD")
    private CellType cellType;

    @Positive(message = "{cell.validation.max.weight.positive}")
    @Schema(description = "Maximum weight capacity in kg", example = "100.0")
    private Double maxWeightKg;

    @Positive(message = "{cell.validation.max.volume.positive}")
    @Schema(description = "Maximum volume capacity in m³", example = "0.5")
    private Double maxVolumeM3;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Size(max = MAX_NOTES_LENGTH, message = "{cell.validation.notes.max}")
    @Schema(description = "Notes", example = "Near the entrance")
    private String notes;
}