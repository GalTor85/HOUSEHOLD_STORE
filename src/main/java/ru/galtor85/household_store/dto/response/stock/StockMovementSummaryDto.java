package ru.galtor85.household_store.dto.response.stock;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Stock movement summary DTO")
public class StockMovementSummaryDto {

    @Schema(description = "Period start", example = "2026-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "Period end", example = "2026-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "Total movements count", example = "150")
    private Long totalMovements;

    @Schema(description = "Receipts count", example = "80")
    private Long receiptsCount;

    @Schema(description = "Shipments count", example = "50")
    private Long shipmentsCount;

    @Schema(description = "Transfers count", example = "15")
    private Long transfersCount;

    @Schema(description = "Write-offs count", example = "5")
    private Long writeOffsCount;

    @Schema(description = "Returns count", example = "0")
    private Long returnsCount;

    @Schema(description = "Total quantity received", example = "5000")
    private Integer totalQuantityReceived;

    @Schema(description = "Total quantity shipped", example = "3500")
    private Integer totalQuantityShipped;

    @Schema(description = "Total quantity transferred", example = "1000")
    private Integer totalQuantityTransferred;

    @Schema(description = "Total quantity written off", example = "50")
    private Integer totalQuantityWrittenOff;

    @Schema(description = "Summary by movement type")
    private Map<String, MovementTypeSummary> byMovementType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Movement type summary")
    public static class MovementTypeSummary {
        private Long count;
        private Integer totalQuantity;
        private BigDecimal totalValue;
    }
}