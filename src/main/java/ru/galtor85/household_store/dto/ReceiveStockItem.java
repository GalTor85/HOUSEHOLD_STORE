package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Receive stock item DTO", title = "Receive Stock Item")
public class ReceiveStockItem {

    @Schema(description = "Product ID", example = "1", required = true)
    private Long productId;

    @Schema(description = "Quantity received", example = "10", required = true)
    private Integer quantity;

    @Schema(description = "Cell ID for placement", example = "5")
    private Long cellId;

    @Schema(description = "Cell code for placement", example = "A-01-01-01")
    private String cellCode;

    // ДОБАВЛЯЕМ ПОЛЕ ДЛЯ НОМЕРА ПАРТИИ
    @Schema(description = "Batch/Lot number", example = "BATCH-20240318-ABC123")
    private String batchNumber;

    @Schema(description = "Expiry date for perishable goods", example = "2025-03-18T00:00:00")
    private LocalDateTime expiryDate;

    @Schema(description = "Manufacturing date", example = "2024-03-18T00:00:00")
    private LocalDateTime manufacturingDate;

    @Schema(description = "Serial number (for individual items)", example = "SN123456789")
    private String serialNumber;

    @Schema(description = "Quality certificate number", example = "QC-2024-001")
    private String qualityCertificateNumber;

    @Schema(description = "Additional notes", example = "Fragile items, handle with care")
    private String notes;

    public boolean hasBatchNumber() {
        return batchNumber != null && !batchNumber.isEmpty();
    }
}