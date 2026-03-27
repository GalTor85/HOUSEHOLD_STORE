package ru.galtor85.household_store.dto.response.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Warehouse DTO", title = "Warehouse")
public class WarehouseDto {

    @Schema(description = "Warehouse ID", example = "1")
    private Long id;

    @Schema(description = "Warehouse code", example = "WH-001")
    private String code;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String name;

    @Schema(description = "Description", example = "Main warehouse for finished goods")
    private String description;

    @Schema(description = "Barcode", example = "1234567890123")
    private String barcode;

    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @Schema(description = "Physical address", example = "123 Storage St, City")
    private String address;

    @Schema(description = "Contact person", example = "John Doe")
    private String contactPerson;

    @Schema(description = "Contact phone", example = "+1234567890")
    private String contactPhone;

    @Schema(description = "Contact email", example = "warehouse@company.com")
    private String contactEmail;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Total capacity", example = "1000")
    private Integer totalCapacity;

    @Schema(description = "Used capacity", example = "450")
    private Integer usedCapacity;

    @Schema(description = "Available cells", example = "550")
    private Integer availableCapacity;

    @Schema(description = "Occupancy percentage", example = "45.5")
    private Double occupancyPercentage;

    @Schema(description = "Storage cells")
    private List<StorageCellDto> cells;

    @Schema(description = "Created at", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
}