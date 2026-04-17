package ru.galtor85.household_store.dto.request.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.stock.MovementType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock movement filter request")
public class StockMovementFilterRequest {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Cell ID", example = "10")
    private Long cellId;

    @Schema(description = "Movement type", example = "RECEIPT")
    private MovementType movementType;

    @Schema(description = "Reference type", example = "PURCHASE")
    private String referenceType;

    @Schema(description = "Reference ID", example = "100")
    private Long referenceId;

    @Schema(description = "Batch number", example = "BATCH-20240417-ABC123")
    private String batchNumber;

    @Schema(description = "Start date", example = "2026-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "End date", example = "2026-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "Page number (0-indexed)", example = "0")
    private Integer page;

    @Schema(description = "Page size", example = "20")
    private Integer size;

    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy;

    @Schema(description = "Sort direction (asc/desc)", example = "desc")
    private String sortDir;
}