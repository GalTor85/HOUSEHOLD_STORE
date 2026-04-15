package ru.galtor85.household_store.dto.request.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for creating a warehouse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Warehouse create request", title = "Warehouse Create Request")
public class WarehouseCreateRequest {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotBlank(message = "{warehouse.validation.code.empty}")
    @Size(max = MAX_WAREHOUSE_CODE_LENGTH, message = "{warehouse.validation.code.max}")
    @Schema(description = "Warehouse code", example = "WH-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "{warehouse.validation.name.empty}")
    @Size(max = MAX_WAREHOUSE_NAME_LENGTH, message = "{warehouse.validation.name.max}")
    @Schema(description = "Warehouse name", example = "Main Warehouse", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{warehouse.validation.address.empty}")
    @Size(max = MAX_ADDRESS_LENGTH, message = "{warehouse.validation.address.max}")
    @Schema(description = "Physical address", example = "123 Storage St, City", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @NotNull(message = "{warehouse.validation.total.capacity.empty}")
    @Positive(message = "{warehouse.validation.total.capacity.positive}")
    @Schema(description = "Total capacity", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCapacity;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{warehouse.validation.description.max}")
    @Schema(description = "Description", example = "Main warehouse for finished goods")
    private String description;

    @Size(max = MAX_BARCODE_LENGTH, message = "{warehouse.validation.barcode.max}")
    @Schema(description = "Barcode", example = "1234567890123")
    private String barcode;

    @Size(max = MAX_BARCODE_FORMAT_LENGTH, message = "{warehouse.validation.barcode.format.max}")
    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @Size(max = MAX_CONTACT_PERSON_LENGTH, message = "{warehouse.validation.contact.person.max}")
    @Schema(description = "Contact person", example = "John Doe")
    private String contactPerson;

    @Size(max = MAX_PHONE_LENGTH, message = "{warehouse.validation.contact.phone.max}")
    @Schema(description = "Contact phone", example = "+1234567890")
    private String contactPhone;

    @Email(message = "{warehouse.validation.contact.email.invalid}")
    @Size(max = MAX_EMAIL_LENGTH, message = "{warehouse.validation.contact.email.max}")
    @Schema(description = "Contact email", example = "warehouse@company.com")
    private String contactEmail;
}