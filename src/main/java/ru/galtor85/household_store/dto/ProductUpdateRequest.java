package ru.galtor85.household_store.dto;

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
@Schema(description = "Product update request DTO", title = "Product Update Request")
public class ProductUpdateRequest {

    @Size(min = 3, max = 50, message = "{product.validation.sku.size}")
    @Schema(description = "SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128")
    private String sku;

    // ДОБАВЛЕНО: штрих-код
    @Size(min = 8, max = 20, message = "{product.validation.barcode.size}")
    @Schema(description = "Barcode (EAN-13, UPC)", example = "4601234567890")
    private String barcode;

    // ДОБАВЛЕНО: формат штрих-кода
    @Schema(description = "Barcode format", example = "EAN_13")
    private String barcodeFormat;

    @Size(min = 2, max = 100, message = "{product.validation.name.size}")
    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String name;

    @Size(max = 500, message = "{product.validation.description.size}")
    @Schema(description = "Product description", example = "Apple iPhone 13 Pro, 128GB, Graphite")
    private String description;

    @DecimalMin(value = "0.01", message = "{product.validation.price.min}")
    @DecimalMax(value = "999999.99", message = "{product.validation.price.max}")
    @Schema(description = "Price", example = "999.99")
    private BigDecimal price;

    @Min(value = 0, message = "{product.validation.quantity.min}")
    @Max(value = 999999, message = "{product.validation.quantity.max}")
    @Schema(description = "Quantity in stock", example = "10")
    private Integer quantityInStock;

    @Size(max = 50, message = "{product.validation.category.size}")
    @Schema(description = "Category", example = "Electronics")
    private String category;

    @Size(max = 50, message = "{product.validation.brand.size}")
    @Schema(description = "Brand", example = "Apple")
    private String brand;

    @Size(max = 255, message = "{product.validation.imageUrl.size}")
    @Schema(description = "Image URL", example = "/images/products/iphone-13-pro.jpg")
    private String imageUrl;

    @Schema(description = "Active status", example = "true")
    private Boolean active;

    @Schema(description = "Product attributes")
    private List<ProductAttributeDto> attributes;

    @Schema(description = "Has variants", example = "false")
    private Boolean hasVariants;

    @Schema(description = "Parent product ID", example = "1")
    private Long parentProductId;
}