package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Receive order request DTO", title = "Receive Order Request")
public class ReceiveOrderRequest {

    @Schema(description = "Actual delivery date/time", example = "2024-02-15T14:30:00")
    private LocalDateTime receivedAt;

    @Schema(description = "Quality check passed", example = "true")
    private Boolean qualityCheck;

    @Schema(description = "Quality check notes", example = "All items in good condition")
    private String qualityNotes;

    @Schema(description = "Payment status after receipt", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Warehouse location where goods were stored", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Schema(description = "List of received items with actual quantities (for partial receipts)")
    private List<ReceivedItemDto> receivedItems;
}