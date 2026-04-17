package ru.galtor85.household_store.dto.response.stock;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Stock transfer response DTO")
public class StockTransferResponseDto {

    @Schema(description = "Source warehouse ID", example = "1")
    private Long fromWarehouseId;

    @Schema(description = "Source warehouse name", example = "Main Warehouse")
    private String fromWarehouseName;

    @Schema(description = "Source cell ID", example = "10")
    private Long fromCellId;

    @Schema(description = "Source cell code", example = "A-01-01-01")
    private String fromCellCode;

    @Schema(description = "Destination warehouse ID", example = "2")
    private Long toWarehouseId;

    @Schema(description = "Destination warehouse name", example = "Secondary Warehouse")
    private String toWarehouseName;

    @Schema(description = "Destination cell ID", example = "20")
    private Long toCellId;

    @Schema(description = "Destination cell code", example = "B-02-03-01")
    private String toCellCode;

    @Schema(description = "Quantity transferred", example = "10")
    private Integer quantity;

    @Schema(description = "Movement ID", example = "100")
    private Long movementId;

    @Schema(description = "Transfer timestamp", example = "2026-04-17T10:30:00")
    private LocalDateTime transferredAt;

    @Schema(description = "Localized success message", example = "10 items successfully transferred from Main Warehouse to Secondary Warehouse")
    private String localizedMessage;
}