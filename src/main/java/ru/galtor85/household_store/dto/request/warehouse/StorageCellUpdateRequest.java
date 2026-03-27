package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage cell update request", title = "Storage Cell Update Request")
public class StorageCellUpdateRequest {

    @Schema(description = "Cell type", example = "STANDARD")
    private CellType cellType;

    @Schema(description = "Section (A, B, C...)", example = "A")
    private String section;

    @Schema(description = "Rack number", example = "01")
    private String rack;

    @Schema(description = "Shelf number", example = "01")
    private String shelf;

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

    @Schema(description = "Notes about the cell", example = "Near the entrance")
    private String notes;
}