package ru.galtor85.household_store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product response DTO", title = "Product Response")
public class ProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "SKU (Stock Keeping Unit)", example = "IPHONE-13-PRO-128")
    private String sku;

    // ДОБАВЛЕНО: штрих-код
    @Schema(description = "Barcode (EAN-13, UPC)", example = "4601234567890")
    private String barcode;

    // ДОБАВЛЕНО: формат штрих-кода
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
    private boolean active;

    @Schema(description = "Product attributes")
    private List<ProductAttributeDto> attributes;

    @Schema(description = "Has variants", example = "false")
    private boolean hasVariants;

    @Schema(description = "Parent product ID", example = "1")
    private Long parentProductId;

    @Schema(description = "Product variants")
    private List<ProductResponse> variants;

    @Schema(description = "Product media files")
    private List<ProductMediaDto> media;

    @Schema(description = "Main image URL", example = "/uploads/products/iphone-13-pro.jpg")
    private String mainImageUrl;

    @Schema(description = "All images")
    private List<ProductMediaDto> images;

    @Schema(description = "Videos")
    private List<ProductMediaDto> videos;
}