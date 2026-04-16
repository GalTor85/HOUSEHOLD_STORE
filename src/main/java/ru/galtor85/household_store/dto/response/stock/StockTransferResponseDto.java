package ru.galtor85.household_store.dto.response.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for stock transfer operation.
 *
 * @author G@LTor85
 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock transfer response")
public class StockTransferResponseDto {

    @Schema(description = "Source warehouse ID")
    private Long fromWarehouseId;

    @Schema(description = "Source warehouse name")
    private String fromWarehouseName;

    @Schema(description = "Source cell ID")
    private Long fromCellId;

    @Schema(description = "Source cell code")
    private String fromCellCode;

    @Schema(description = "Destination warehouse ID")
    private Long toWarehouseId;

    @Schema(description = "Destination warehouse name")
    private String toWarehouseName;

    @Schema(description = "Destination cell ID")
    private Long toCellId;

    @Schema(description = "Destination cell code")
    private String toCellCode;

    @Schema(description = "Quantity transferred", example = "10")
    private Integer quantity;

    @Schema(description = "Movement ID")
    private Long movementId;

    @Schema(description = "Transfer timestamp")
    private LocalDateTime transferredAt;

    @Schema(description = "Localized success message")
    private String localizedMessage;
}