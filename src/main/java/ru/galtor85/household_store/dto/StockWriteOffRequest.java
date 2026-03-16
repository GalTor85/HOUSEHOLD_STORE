package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock write-off request DTO", title = "Stock Write-Off Request")
public class StockWriteOffRequest {

    @NotEmpty(message = "{writeoff.validation.items.empty}")
    @Schema(description = "List of items to write off", required = true)
    private List<StockWriteOffItem> items;

    @NotNull(message = "{writeoff.validation.reason.empty}")
    @Schema(description = "Reason for write-off", example = "DAMAGED", required = true)
    private String reason;

    @Schema(description = "Detailed description", example = "Damaged during transportation")
    private String description;

    @Schema(description = "Warehouse location", example = "Warehouse A")
    private String warehouseLocation;

    @Schema(description = "Related order ID (if applicable)", example = "1")
    private Long relatedOrderId;
}