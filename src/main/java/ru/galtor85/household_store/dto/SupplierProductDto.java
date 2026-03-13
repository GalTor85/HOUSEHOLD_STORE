package ru.galtor85.household_store.dto;

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
@Schema(description = "Supplier product DTO", title = "Supplier Product")
public class SupplierProductDto {

    private Long id;
    private Long supplierId;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal supplierPrice;
    private String supplierSku;
    private boolean mainSupplier;
    private Integer deliveryTime;
    private Integer minOrderQuantity;
}