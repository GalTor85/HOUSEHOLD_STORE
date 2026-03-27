package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.PurchaseOrderItemCreateDto;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Purchase salesOrder create request")
public class PurchaseOrderCreateRequest {

    @NotNull(message = "{purchase.validation.supplier.id.empty}")
    @Positive(message = "{purchase.validation.supplier.id.positive}")
    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @NotEmpty(message = "{purchase.validation.items.empty}")
    @Valid
    @Schema(description = "Items to purchase")
    private List<PurchaseOrderItemCreateDto> items;  // ← ИЗМЕНЕНО!

    @Schema(description = "Expected delivery date", example = "2024-03-25")
    private LocalDate expectedDelivery;

    @Schema(description = "Warehouse location", example = "Warehouse A")
    private String warehouseLocation;

    @Schema(description = "Invoice number", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Payment due date", example = "2024-04-10")
    private LocalDate paymentDue;

    @Schema(description = "Notes")
    private String notes;
}