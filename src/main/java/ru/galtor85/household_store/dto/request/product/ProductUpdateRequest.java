package ru.galtor85.household_store.dto.request.product;

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
}