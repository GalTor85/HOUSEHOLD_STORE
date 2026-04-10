package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Purchase order create request")
public class PurchaseOrderCreateRequest {

    @NotNull(message = "{purchase.validation.supplier.id.empty}")
    @Positive(message = "{purchase.validation.supplier.id.positive}")
    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @NotEmpty(message = "{purchase.validation.items.empty}")
    @Valid
    @Schema(description = "Items to purchase")
    private List<PurchaseOrderItemCreateDto> items;

    @Schema(description = "Expected delivery date", example = "2024-03-25")
    private LocalDate expectedDelivery;

    @Size(max = 100, message = "{purchase.validation.warehouse.location.max}")
    @Schema(description = "Warehouse location", example = "Warehouse A")
    private String warehouseLocation;

    @Size(max = 50, message = "{purchase.validation.invoice.number.max}")
    @Schema(description = "Invoice number", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Payment due date", example = "2024-04-10")
    private LocalDate paymentDue;

    @Size(max = 1000, message = "{purchase.validation.notes.max}")
    @Schema(description = "Notes")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasExpectedDelivery() {
        return expectedDelivery != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWarehouseLocation() {
        return warehouseLocation != null && !warehouseLocation.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasInvoiceNumber() {
        return invoiceNumber != null && !invoiceNumber.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPaymentDue() {
        return paymentDue != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }
}