package ru.galtor85.household_store.dto.request.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for supplier product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier product request DTO", title = "Supplier Product Request")
public class SupplierProductRequest {

    // =========================================================================
    // REQUIRED FIELDS
    // =========================================================================

    @NotNull(message = "{supplier.product.validation.price.empty}")
    @Positive(message = "{supplier.product.validation.price.positive}")
    @Schema(description = "Supplier price for the product", example = "850.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal supplierPrice;

    @NotBlank(message = "{supplier.product.validation.sku.empty}")
    @Size(max = MAX_SUPPLIER_SKU_LENGTH, message = "{supplier.product.validation.sku.max}")
    @Schema(description = "Supplier's SKU for the product", example = "SUP-IPHONE-128", requiredMode = Schema.RequiredMode.REQUIRED)
    private String supplierSku;

    // =========================================================================
    // OPTIONAL FIELDS
    // =========================================================================

    @Schema(description = "Is this the main supplier for this product", example = "true", defaultValue = "false")
    private Boolean mainSupplier;

    @Positive(message = "{supplier.product.validation.delivery.time.positive}")
    @Schema(description = "Delivery time in days from this supplier", example = "3")
    private Integer deliveryTime;

    @PositiveOrZero(message = "{supplier.product.validation.min.order.quantity.positive.or.zero}")
    @Schema(description = "Minimum order quantity", example = "10")
    private Integer minOrderQuantity;
}