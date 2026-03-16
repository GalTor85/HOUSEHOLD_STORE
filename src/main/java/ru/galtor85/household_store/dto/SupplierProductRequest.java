package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier product request DTO", title = "Supplier Product Request")
public class SupplierProductRequest {

    @NotNull(message = "{supplier.product.validation.price.empty}")
    @Positive(message = "{supplier.product.validation.price.positive}")
    @Schema(description = "Supplier price for the product", example = "850.00", required = true)
    private BigDecimal supplierPrice;

    @NotBlank(message = "{supplier.product.validation.sku.empty}")
    @Schema(description = "Supplier's SKU for the product", example = "SUP-IPHONE-128", required = true)
    private String supplierSku;

    // ИСПРАВЛЕНО: boolean -> Boolean
    @Schema(description = "Is this the main supplier for this product", example = "true", defaultValue = "false")
    private Boolean mainSupplier;

    @Positive(message = "{supplier.product.validation.delivery.time.positive}")
    @Schema(description = "Delivery time in days from this supplier", example = "3")
    private Integer deliveryTime;

    @PositiveOrZero(message = "{supplier.product.validation.min.order.quantity.positive.or.zero}")
    @Schema(description = "Minimum order quantity", example = "10")
    private Integer minOrderQuantity;

    @Schema(description = "Notes about this product from supplier", example = "Original packaging, 1 year warranty")
    private String notes;

    @Schema(description = "Supplier's product category", example = "Smartphones")
    private String supplierCategory;

    @Schema(description = "Supplier's product name (if different)", example = "iPhone 13 Pro 128GB Graphite")
    private String supplierProductName;

    @Schema(description = "Country of origin", example = "China")
    private String countryOfOrigin;

    @Schema(description = "HS Code (Harmonized System code for customs)", example = "8517.12.00")
    private String hsCode;

    @Schema(description = "Is product in stock at supplier", example = "true")
    private Boolean inStockAtSupplier;

    @Schema(description = "Supplier's stock quantity (if available)", example = "500")
    private Integer supplierStockQuantity;

    @Schema(description = "Estimated restock date if out of stock", example = "2024-03-15")
    private String estimatedRestockDate;
}