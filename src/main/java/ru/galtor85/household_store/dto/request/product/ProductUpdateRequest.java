package ru.galtor85.household_store.dto.request.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for updating a product.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product update request DTO", title = "Product Update Request")
public class ProductUpdateRequest {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Size(min = MIN_SKU_LENGTH, max = MAX_SKU_LENGTH, message = "{product.validation.sku.size}")
    @Schema(description = "SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128")
    private String sku;

    @Size(min = MIN_BARCODE_LENGTH, max = MAX_BARCODE_LENGTH, message = "{product.validation.barcode.size}")
    @Schema(description = "Barcode (EAN-13, UPC)", example = "4601234567890")
    private String barcode;

    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    // =========================================================================
    // BASIC INFORMATION
    // =========================================================================

    @Size(min = MIN_NAME_LENGTH, max = MAX_PRODUCT_NAME_LENGTH, message = "{product.validation.name.size}")
    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "{product.validation.description.size}")
    @Schema(description = "Product description", example = "Apple iPhone 13 Pro, 128GB, Graphite")
    private String description;

    // =========================================================================
    // PRICE AND STOCK
    // =========================================================================

    @DecimalMin(value = MIN_PRICE_STR, message = "{product.validation.price.min}")
    @DecimalMax(value = MAX_PRICE_STR, message = "{product.validation.price.max}")
    @Schema(description = "Price", example = "999.99")
    private BigDecimal price;

    @Min(value = MIN_QUANTITY, message = "{product.validation.quantity.min}")
    @Max(value = MAX_QUANTITY, message = "{product.validation.quantity.max}")
    @Schema(description = "Quantity in stock", example = "10")
    private Integer quantityInStock;

    // =========================================================================
    // CLASSIFICATION
    // =========================================================================

    @Size(max = MAX_CATEGORY_LENGTH, message = "{product.validation.category.size}")
    @Schema(description = "Category", example = "Electronics")
    private String category;

    @Size(max = MAX_BRAND_LENGTH, message = "{product.validation.brand.size}")
    @Schema(description = "Brand", example = "Apple")
    private String brand;

    @Size(max = MAX_URL_LENGTH, message = "{product.validation.imageUrl.size}")
    @Schema(description = "Image URL", example = "/images/products/iphone-13-pro.jpg")
    private String imageUrl;

    // =========================================================================
    // STATUS
    // =========================================================================

    @Schema(description = "Active status", example = "true")
    private Boolean active;

    @Schema(description = "Has variants", example = "false")
    private Boolean hasVariants;

    // =========================================================================
    // RELATIONSHIPS
    // =========================================================================

    @Schema(description = "Product attributes to update")
    private List<AttributeUpdateRequest> attributes;

    // =========================================================================
    // PHYSICAL PROPERTIES
    // =========================================================================

    @DecimalMin(value = MIN_WEIGHT_STR, message = "{product.validation.weight.min}")
    @DecimalMax(value = MAX_WEIGHT_STR, message = "{product.validation.weight.max}")
    @Schema(description = "Weight in kg", example = "0.5")
    private Double weightKg;

    @DecimalMin(value = MIN_VOLUME_STR, message = "{product.validation.volume.min}")
    @DecimalMax(value = MAX_VOLUME_STR, message = "{product.validation.volume.max}")
    @Schema(description = "Volume in cubic meters", example = "0.001")
    private Double volumeM3;

    // =========================================================================
    // STORAGE REQUIREMENTS
    // =========================================================================

    @Schema(description = "Requires refrigeration", example = "false")
    private Boolean requiresRefrigeration;

    @Schema(description = "Requires freezing", example = "false")
    private Boolean requiresFreezing;

    @Schema(description = "Is hazardous material", example = "false")
    private Boolean hazardous;

    @Schema(description = "Is oversized", example = "false")
    private Boolean oversize;

    @Schema(description = "Is liquid", example = "false")
    private Boolean liquid;

    @Schema(description = "Is palletized", example = "false")
    private Boolean palletized;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasSku() {
        return sku != null && !sku.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBarcode() {
        return barcode != null && !barcode.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBarcodeFormat() {
        return barcodeFormat != null && !barcodeFormat.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPrice() {
        return price != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasQuantityInStock() {
        return quantityInStock != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBrand() {
        return brand != null && !brand.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasActive() {
        return active != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasHasVariants() {
        return hasVariants != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWeightKg() {
        return weightKg != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasVolumeM3() {
        return volumeM3 != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasRequiresRefrigeration() {
        return requiresRefrigeration != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasRequiresFreezing() {
        return requiresFreezing != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasHazardous() {
        return hazardous != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasOversize() {
        return oversize != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasLiquid() {
        return liquid != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPalletized() {
        return palletized != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean isActiveTrue() {
        return Boolean.TRUE.equals(active);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasVariantsTrue() {
        return Boolean.TRUE.equals(hasVariants);
    }
}