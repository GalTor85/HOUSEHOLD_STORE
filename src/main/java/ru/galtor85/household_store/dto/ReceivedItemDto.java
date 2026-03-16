package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Received item DTO", title = "Received Item")
public class ReceivedItemDto {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Quantity actually received", example = "95")
    private Integer receivedQuantity;

    @Schema(description = "Quantity damaged/rejected", example = "5")
    private Integer damagedQuantity;

    @Schema(description = "Quality check result", example = "true")
    private Boolean qualityPassed;

    @Schema(description = "Notes about this item", example = "5 units with damaged packaging")
    private String notes;
}