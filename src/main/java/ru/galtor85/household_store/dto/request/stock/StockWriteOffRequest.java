package ru.galtor85.household_store.dto.request.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.StockWriteOffItem;

import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for stock write-off.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock write-off request DTO", title = "Stock Write-Off Request")
public class StockWriteOffRequest {

    @NotEmpty(message = "{writeoff.validation.items.empty}")
    @Schema(description = "List of items to write off", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StockWriteOffItem> items;

    @NotNull(message = "{writeoff.validation.reason.empty}")
    @Size(max = MAX_REASON_WRITEOFF_LENGTH, message = "{writeoff.validation.reason.max}")
    @Schema(description = "Reason for write-off", example = "DAMAGED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "Warehouse ID for write-off", example = "1")
    private Long warehouseId;

    @Schema(description = "Related order ID (if applicable)", example = "1")
    private Long relatedOrderId;
}