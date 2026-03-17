package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product update request DTO", title = "Product Update Request")
public class ProductUpdateRequest {

    @Schema(description = "SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128")
    private String sku;

    @Schema(description = "Barcode (EAN-13, UPC)", example = "4601234567890")
    private String barcode;

    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String name;

    @Schema(description = "Product description", example = "Apple iPhone 13 Pro, 128GB, Graphite")
    private String description;

    @Schema(description = "Price", example = "999.99")
    private BigDecimal price;

    @Schema(description = "Quantity in stock", example = "10")
    private Integer quantityInStock;

    @Schema(description = "Category", example = "Electronics")
    private String category;

    @Schema(description = "Brand", example = "Apple")
    private String brand;

    @Schema(description = "Image URL", example = "/images/products/iphone-13-pro.jpg")
    private String imageUrl;

    @Schema(description = "Active status", example = "true")
    private Boolean active;

    @Schema(description = "Has variants", example = "false")
    private Boolean hasVariants;


    @Schema(description = "Product attributes to update")
    private List<AttributeUpdateRequest> attributes;

    @Schema(description = "Weight in kg", example = "0.5")
    private Double weightKg;

    @Schema(description = "Volume in cubic meters", example = "0.001")
    private Double volumeM3;

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