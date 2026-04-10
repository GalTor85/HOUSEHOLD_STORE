package ru.galtor85.household_store.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_BATCH_NUMBER_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_CELL_CODE_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_NOTES_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_SERIAL_NUMBER_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MAX_QUALITY_CERTIFICATE_LENGTH;
import static ru.galtor85.household_store.constants.TechnicalConstants.MIN_QUANTITY;

/**
 * DTO for receive stock item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Receive stock item DTO", title = "Receive Stock Item")
public class ReceiveStockItem {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotNull(message = "{receive.validation.product.id.empty}")
    @Positive(message = "{receive.validation.product.id.positive}")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull(message = "{receive.validation.quantity.empty}")
    @Positive(message = "{receive.validation.quantity.positive}")
    @Schema(description = "Quantity received", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Positive(message = "{receive.validation.cell.id.positive}")
    @Schema(description = "Cell ID for placement", example = "5")
    private Long cellId;

    @Size(max = MAX_CELL_CODE_LENGTH, message = "{receive.validation.cell.code.max}")
    @Schema(description = "Cell code for placement", example = "A-01-01-01")
    private String cellCode;

    @Size(max = MAX_BATCH_NUMBER_LENGTH, message = "{receive.validation.batch.number.max}")
    @Schema(description = "Batch/Lot number", example = "BATCH-20240318-ABC123")
    private String batchNumber;

    @Schema(description = "Expiry date for perishable goods", example = "2025-03-18T00:00:00")
    private LocalDateTime expiryDate;

    @Schema(description = "Manufacturing date", example = "2024-03-18T00:00:00")
    private LocalDateTime manufacturingDate;

    @Size(max = MAX_SERIAL_NUMBER_LENGTH, message = "{receive.validation.serial.number.max}")
    @Schema(description = "Serial number (for individual items)", example = "SN123456789")
    private String serialNumber;

    @Size(max = MAX_QUALITY_CERTIFICATE_LENGTH, message = "{receive.validation.quality.certificate.max}")
    @Schema(description = "Quality certificate number", example = "QC-2024-001")
    private String qualityCertificateNumber;

    @Size(max = MAX_NOTES_LENGTH, message = "{receive.validation.notes.max}")
    @Schema(description = "Additional notes", example = "Fragile items, handle with care")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasProductId() {
        return productId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQuantity() {
        return quantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCellId() {
        return cellId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCellCode() {
        return cellCode != null && !cellCode.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBatchNumber() {
        return batchNumber != null && !batchNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasExpiryDate() {
        return expiryDate != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasManufacturingDate() {
        return manufacturingDate != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSerialNumber() {
        return serialNumber != null && !serialNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQualityCertificateNumber() {
        return qualityCertificateNumber != null && !qualityCertificateNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidQuantity() {
        return quantity != null && quantity > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCellCode() {
        return cellCode != null ? cellCode.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedBatchNumber() {
        return batchNumber != null ? batchNumber.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSerialNumber() {
        return serialNumber != null ? serialNumber.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedQualityCertificateNumber() {
        return qualityCertificateNumber != null ? qualityCertificateNumber.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedNotes() {
        return notes != null ? notes.trim() : null;
    }
}