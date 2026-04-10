package ru.galtor85.household_store.dto.request.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Size(max = MAX_NOTES_LENGTH, message = "{supplier.product.validation.notes.max}")
    @Schema(description = "Notes about this product from supplier", example = "Original packaging, 1 year warranty")
    private String notes;

    @Size(max = MAX_SUPPLIER_CATEGORY_LENGTH, message = "{supplier.product.validation.category.max}")
    @Schema(description = "Supplier's product category", example = "Smartphones")
    private String supplierCategory;

    @Size(max = MAX_SUPPLIER_PRODUCT_NAME_LENGTH, message = "{supplier.product.validation.product.name.max}")
    @Schema(description = "Supplier's product name (if different)", example = "iPhone 13 Pro 128GB Graphite")
    private String supplierProductName;

    @Size(max = MAX_COUNTRY_NAME_LENGTH, message = "{supplier.product.validation.country.of.origin.max}")
    @Schema(description = "Country of origin", example = "China")
    private String countryOfOrigin;

    @Size(max = MAX_HS_CODE_LENGTH, message = "{supplier.product.validation.hs.code.max}")
    @Schema(description = "HS Code (Harmonized System code for customs)", example = "8517.12.00")
    private String hsCode;

    @Schema(description = "Is product in stock at supplier", example = "true")
    private Boolean inStockAtSupplier;

    @PositiveOrZero(message = "{supplier.product.validation.supplier.stock.quantity.positive.or.zero}")
    @Schema(description = "Supplier's stock quantity (if available)", example = "500")
    private Integer supplierStockQuantity;

    @Pattern(regexp = DATE_PATTERN, message = "{supplier.product.validation.estimated.restock.date.invalid}")
    @Schema(description = "Estimated restock date if out of stock", example = "2024-03-15")
    private String estimatedRestockDate;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isMainSupplierTrue() {
        return Boolean.TRUE.equals(mainSupplier);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDeliveryTime() {
        return deliveryTime != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMinOrderQuantity() {
        return minOrderQuantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSupplierCategory() {
        return supplierCategory != null && !supplierCategory.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSupplierProductName() {
        return supplierProductName != null && !supplierProductName.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCountryOfOrigin() {
        return countryOfOrigin != null && !countryOfOrigin.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasHsCode() {
        return hsCode != null && !hsCode.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isInStockAtSupplierTrue() {
        return Boolean.TRUE.equals(inStockAtSupplier);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSupplierStockQuantity() {
        return supplierStockQuantity != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasEstimatedRestockDate() {
        return estimatedRestockDate != null && !estimatedRestockDate.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSupplierSku() {
        return supplierSku != null ? supplierSku.trim().toUpperCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedNotes() {
        return notes != null ? notes.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSupplierCategory() {
        return supplierCategory != null ? supplierCategory.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedSupplierProductName() {
        return supplierProductName != null ? supplierProductName.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedCountryOfOrigin() {
        return countryOfOrigin != null ? countryOfOrigin.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedHsCode() {
        return hsCode != null ? hsCode.trim().toUpperCase() : null;
    }
}