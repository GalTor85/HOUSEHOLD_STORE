package ru.galtor85.household_store.dto.response.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.warehouse.CellType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage cell DTO", title = "Storage Cell")
public class StorageCellDto {

    @Schema(description = "Cell ID", example = "1")
    private Long id;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Cell code", example = "A-01-01")
    private String code;

    @Schema(description = "Barcode", example = "CELL-001-123456")
    private String barcode;

    @Schema(description = "Barcode format", example = "CODE_128")
    private String barcodeFormat;

    @Schema(description = "Section", example = "A")
    private String section;

    @Schema(description = "Rack", example = "01")
    private String rack;

    @Schema(description = "Shelf", example = "01")
    private String shelf;

    @Schema(description = "Position", example = "01")
    private String position;

    @Schema(description = "Cell type", example = "STANDARD")
    private CellType cellType;

    @Schema(description = "Max weight (kg)", example = "100.0")
    private Double maxWeightKg;

    @Schema(description = "Max volume (m³)", example = "0.5")
    private Double maxVolumeM3;

    @Schema(description = "Current product ID", example = "123")
    private Long currentProductId;

    @Schema(description = "Current product name", example = "iPhone 13 Pro")
    private String currentProductName;

    @Schema(description = "Current quantity", example = "50")
    private Integer currentQuantity;

    @Schema(description = "Is occupied", example = "true")
    private Boolean isOccupied;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Full location path", example = "Main Warehouse > Section A > Rack 01 > Shelf 01 > Position 01")
    private String fullLocationPath;

    @Schema(description = "Last inventory date", example = "2024-01-15T10:00:00")
    private LocalDateTime lastInventoryDate;

    @Schema(description = "Notes", example = "Near the entrance")
    private String notes;
}