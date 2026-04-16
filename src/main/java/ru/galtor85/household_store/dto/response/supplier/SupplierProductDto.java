package ru.galtor85.household_store.dto.response.supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Supplier product DTO", title = "Supplier Product")
public class SupplierProductDto {

    @Schema(description = "Supplier product ID", example = "1")
    private Long id;

    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @Schema(description = "Supplier name", example = "OOO TechnoPost")
    private String supplierName;

    @Schema(description = "Product ID", example = "1")
    private Long productId;


    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;


    @Schema(description = "Product SKU", example = "IPHONE-13-PRO-128")
    private String productSku;

    @Schema(description = "Supplier price", example = "850.00")
    private BigDecimal supplierPrice;

    @Schema(description = "Supplier SKU", example = "SUP-IPHONE-128")
    private String supplierSku;

    @Schema(description = "Is main supplier", example = "true")
    private boolean mainSupplier;

    @Schema(description = "Delivery time in days", example = "3")
    private Integer deliveryTime;

    @Schema(description = "Minimum salesOrder quantity", example = "10")
    private Integer minOrderQuantity;
}