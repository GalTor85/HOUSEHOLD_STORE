package ru.galtor85.household_store.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

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
}