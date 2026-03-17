package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "{receive.validation.product.id.empty}")
    @Schema(description = "Product ID", example = "1", required = true)
    private Long productId;

    @NotNull(message = "{receive.validation.quantity.empty}")
    @Min(value = 1, message = "{receive.validation.quantity.min}")
    @Schema(description = "Quantity received", example = "10", required = true)
    private Integer quantity;

    @Schema(description = "Cell ID for placement (if known)", example = "5")
    private Long cellId;

    @Schema(description = "Cell code for placement (alternative to cellId)",
            example = "A-01-01-01")
    private String cellCode;

    @Schema(description = "Batch/Lot number", example = "BATCH-2024-001")
    private String batchNumber;

    @Schema(description = "Expiry date for perishable goods", example = "2025-03-17T00:00:00")
    private LocalDateTime expiryDate;

    @Schema(description = "Manufacturing date", example = "2024-03-17T00:00:00")
    private LocalDateTime manufacturingDate;

    @Schema(description = "Serial number (for individual items)", example = "SN123456789")
    private String serialNumber;

    @Schema(description = "Country of origin", example = "China")
    private String countryOfOrigin;

    @Schema(description = "Customs declaration number", example = "CD-2024-12345")
    private String customsDeclarationNumber;

    @Schema(description = "Quality certificate number", example = "QC-2024-001")
    private String qualityCertificateNumber;

    @Schema(description = "Additional notes", example = "Fragile items, handle with care")
    private String notes;

    // ========== Вспомогательные методы ==========

    /**
     * Проверка, указан ли конкретный идентификатор ячейки
     */
    public boolean hasCellIdentifier() {
        return cellId != null || (cellCode != null && !cellCode.isEmpty());
    }

    /**
     * Проверка, является ли товар просроченным на текущую дату
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    /**
     * Проверка, требуется ли отслеживание по серийным номерам
     */
    public boolean requiresSerialTracking() {
        return serialNumber != null && !serialNumber.isEmpty();
    }

    /**
     * Проверка, требуется ли отслеживание по партиям
     */
    public boolean requiresBatchTracking() {
        return batchNumber != null && !batchNumber.isEmpty();
    }

    /**
     * Проверка, является ли товар импортным
     */
    public boolean isImported() {
        return countryOfOrigin != null && !countryOfOrigin.isEmpty();
    }
}