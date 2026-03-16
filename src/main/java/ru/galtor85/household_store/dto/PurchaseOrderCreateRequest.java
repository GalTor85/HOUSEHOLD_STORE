package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Purchase order create request DTO", title = "Purchase Order Create Request")
public class PurchaseOrderCreateRequest {

    @NotNull(message = "{purchase.validation.supplier.id.empty}")
    @Positive(message = "{purchase.validation.supplier.id.positive}")
    @Schema(description = "Supplier ID", example = "1", required = true)
    private Long supplierId;

    @NotEmpty(message = "{purchase.validation.items.empty}")
    @Valid
    @Schema(description = "List of items to purchase", required = true)
    private List<PurchaseOrderItemDto> items;

    @Schema(description = "Expected delivery date", example = "2024-02-15")
    private LocalDate expectedDelivery;

    @Schema(description = "Warehouse location where goods will be received", example = "Warehouse A, Section 3")
    private String warehouseLocation;

    @Schema(description = "Invoice number from supplier", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Payment due date", example = "2024-03-01")
    private LocalDate paymentDue;

    @Schema(description = "Notes or comments for the purchase order")
    private String notes;

    @Schema(description = "Shipping address for delivery", example = "123 Business Ave, Moscow")
    private String shippingAddress;

    @Schema(description = "Contact person at warehouse", example = "Ivan Petrov")
    private String warehouseContact;

    @Schema(description = "Warehouse contact phone", example = "+7 (495) 123-45-67")
    private String warehousePhone;

    @Schema(description = "Purchase order reference number from supplier", example = "PO-12345")
    private String supplierReference;

    @Schema(description = "Currency code", example = "RUB", defaultValue = "RUB")
    private String currency;

    @Schema(description = "Exchange rate (if foreign currency)", example = "1.0")
    private BigDecimal exchangeRate;
}