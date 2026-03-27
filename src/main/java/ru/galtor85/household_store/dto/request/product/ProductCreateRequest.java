package ru.galtor85.household_store.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product create request DTO", title = "Product Create Request")
public class ProductCreateRequest {

    @NotBlank(message = "{product.validation.sku.empty}")
    @Size(min = 3, max = 50, message = "{product.validation.sku.size}")
    @Schema(description = "SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128", required = true)
    private String sku;

    @Size(min = 8, max = 20, message = "{product.validation.barcode.size}")
    @Schema(description = "Barcode (EAN-13, UPC)", example = "4601234567890")
    private String barcode;

    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @NotBlank(message = "{product.validation.name.empty}")
    @Size(min = 2, max = 100, message = "{product.validation.name.size}")
    @Schema(description = "Product name", example = "iPhone 13 Pro", required = true)
    private String name;

    @Size(max = 500, message = "{product.validation.description.size}")
    @Schema(description = "Product description", example = "Apple iPhone 13 Pro, 128GB, Graphite")
    private String description;

    @NotNull(message = "{product.validation.price.empty}")
    @DecimalMin(value = "0.01", message = "{product.validation.price.min}")
    @DecimalMax(value = "999999.99", message = "{product.validation.price.max}")
    @Schema(description = "Price", example = "999.99", required = true)
    private BigDecimal price;

    @Min(value = 0, message = "{product.validation.quantity.min}")
    @Max(value = 999999, message = "{product.validation.quantity.max}")
    @Schema(description = "Quantity in stock", example = "10", defaultValue = "0")
    private Integer quantityInStock = 0;  // Значение по умолчанию

    @Size(max = 50, message = "{product.validation.category.size}")
    @Schema(description = "Category", example = "Electronics")
    private String category;

    @Size(max = 50, message = "{product.validation.brand.size}")
    @Schema(description = "Brand", example = "Apple")
    private String brand;

    @Size(max = 255, message = "{product.validation.imageUrl.size}")
    @Schema(description = "Image URL", example = "/images/products/iphone-13-pro.jpg")
    private String imageUrl;

    @Schema(description = "Active status", example = "true", defaultValue = "true")
    private Boolean active = true;  // Значение по умолчанию

    @Schema(description = "Product attributes")
    private List<AttributeCreateRequest> attributes;

    @Schema(description = "Has variants", example = "false", defaultValue = "false")
    private Boolean hasVariants = false;  // Значение по умолчанию

    @Schema(description = "Parent product ID", example = "1")
    private Long parentProductId;

    @Schema(description = "Product variants", defaultValue = "[]")
    private List<ProductCreateRequest> variants;  // По умолчанию null

    @Schema(description = "Weight in kg", example = "0.5", defaultValue = "0.0")
    private Double weightKg;  // Значение по умолчанию

    @Schema(description = "Volume in cubic meters", example = "0.001", defaultValue = "0.0")
    private Double volumeM3;  // Значение по умолчанию

    @Schema(description = "Requires refrigeration", example = "false", defaultValue = "false")
    private boolean requiresRefrigeration = false;  // Значение по умолчанию

    @Schema(description = "Requires freezing", example = "false", defaultValue = "false")
    private boolean requiresFreezing = false;  // Значение по умолчанию

    @Schema(description = "Is hazardous material", example = "false", defaultValue = "false")
    private boolean hazardous = false;  // Переименовано с isHazardous на hazardous

    @Schema(description = "Is oversized", example = "false", defaultValue = "false")
    private boolean oversize = false;  // Переименовано с isOversize на oversize

    @Schema(description = "Is liquid", example = "false", defaultValue = "false")
    private boolean liquid = false;  // Переименовано с isLiquid на liquid

    @Schema(description = "Is palletized", example = "false", defaultValue = "false")
    private boolean palletized = false;  // Переименовано с isPalletized на palletized
}