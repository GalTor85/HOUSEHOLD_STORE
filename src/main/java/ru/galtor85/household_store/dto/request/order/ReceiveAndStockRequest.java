package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.ReceiveStockItem;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Receive purchase order and stock request",
        title = "Receive and Stock Request")
public class ReceiveAndStockRequest {

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Payment status", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Warehouse ID for receiving", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse location", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Valid
    @NotEmpty(message = "{receive.validation.items.empty}")
    @Schema(description = "Items to receive and stock", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReceiveStockItem> items;

    @Schema(description = "Receiving notes", example = "All items in good condition")
    private String notes;
}
