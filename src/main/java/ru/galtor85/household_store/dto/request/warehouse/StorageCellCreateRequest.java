package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage cell create request", title = "Storage Cell Create Request")
public class StorageCellCreateRequest {

    @NotBlank(message = "{cell.validation.code.empty}")
    @Pattern(regexp = "^[A-Z0-9]+(-[A-Z0-9]+)*$", message = "{cell.validation.code.pattern}")
    @Schema(description = "Cell code (format: SECTION-RACK-SHELF-POSITION)",
            example = "A-01-01-01", required = true)
    private String code;

    @NotNull(message = "{cell.validation.type.empty}")
    @Schema(description = "Cell type", example = "STANDARD", required = true)
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

    @Schema(description = "Cell is active", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @Schema(description = "Notes about the cell", example = "Near the entrance, suitable for fragile items")
    private String notes;

    /**
     * Получить полный путь ячейки в формате Section-Rack-Shelf-Position
     */
    public String getFullPath() {
        StringBuilder path = new StringBuilder();
        if (section != null) path.append(section);
        if (rack != null) path.append("-").append(rack);
        if (shelf != null) path.append("-").append(shelf);
        if (position != null) path.append("-").append(position);
        return path.toString();
    }

    /**
     * Проверка, имеет ли ячейка полную адресацию (секция+стеллаж+полка+позиция)
     */
    public boolean hasFullAddress() {
        return section != null && rack != null && shelf != null && position != null;
    }

    /**
     * Проверка, является ли ячейка паллетным местом
     */
    public boolean isPalletCell() {
        return CellType.PALLET.equals(cellType);
    }

    /**
     * Проверка, требует ли ячейка специальных условий
     */
    public boolean requiresSpecialConditions() {
        return cellType == CellType.FRIDGE ||
                cellType == CellType.FREEZER ||
                cellType == CellType.DANGEROUS;
    }
}