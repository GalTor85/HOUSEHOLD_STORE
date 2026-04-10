package ru.galtor85.household_store.dto.request.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCode() {
        return code != null && !code.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasTotalCapacity() {
        return totalCapacity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBarcode() {
        return barcode != null && !barcode.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBarcodeFormat() {
        return barcodeFormat != null && !barcodeFormat.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasContactPerson() {
        return contactPerson != null && !contactPerson.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasContactPhone() {
        return contactPhone != null && !contactPhone.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasContactEmail() {
        return contactEmail != null && !contactEmail.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCode() {
        return code != null ? code.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedName() {
        return name != null ? name.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedAddress() {
        return address != null ? address.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedDescription() {
        return description != null ? description.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedBarcode() {
        return barcode != null ? barcode.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedContactPerson() {
        return contactPerson != null ? contactPerson.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedContactPhone() {
        return contactPhone != null ? contactPhone.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedContactEmail() {
        return contactEmail != null ? contactEmail.trim().toLowerCase() : null;
    }
}