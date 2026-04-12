package ru.galtor85.household_store.dto.response.stock;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for product availability information displayed to customers.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product availability DTO for customer view")
public class ProductAvailabilityDto {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product name", example = "iPhone 13 Pro")
    private String productName;

    @Schema(description = "Is product in stock", example = "true")
    private boolean inStock;

    @Schema(description = "Available quantity", example = "15")
    private Integer availableQuantity;

    @Schema(description = "Stock status code", example = "IN_STOCK",
            allowableValues = {"IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"})
    private String status;

    @Schema(description = "Localized stock status", example = "В наличии")
    private String localizedStatus;

    @Schema(description = "Localized availability message", example = "Доступно: 15 шт.")
    private String localizedMessage;
}