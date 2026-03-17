package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Warehouse create request", title = "Warehouse Create Request")
public class WarehouseCreateRequest {

    @NotBlank(message = "{warehouse.validation.code.empty}")
    @Schema(description = "Warehouse code", example = "WH-001", required = true)
    private String code;

    @NotBlank(message = "{warehouse.validation.name.empty}")
    @Schema(description = "Warehouse name", example = "Main Warehouse", required = true)
    private String name;

    @Schema(description = "Description", example = "Main warehouse for finished goods")
    private String description;

    @Schema(description = "Barcode", example = "1234567890123")
    private String barcode;

    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @NotBlank(message = "{warehouse.validation.address.empty}")
    @Schema(description = "Physical address", example = "123 Storage St, City", required = true)
    private String address;

    @Schema(description = "Contact person", example = "John Doe")
    private String contactPerson;

    @Schema(description = "Contact phone", example = "+1234567890")
    private String contactPhone;

    @Schema(description = "Contact email", example = "warehouse@company.com")
    private String contactEmail;

    @NotNull(message = "{warehouse.validation.total.capacity.empty}")
    @Positive(message = "{warehouse.validation.total.capacity.positive}")
    @Schema(description = "Total capacity", example = "1000", required = true)
    private Integer totalCapacity;
}