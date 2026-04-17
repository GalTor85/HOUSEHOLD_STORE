package ru.galtor85.household_store.dto.response.product;

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
@Schema(description = "Category statistics DTO")
public class CategoryStatsDto {

    @Schema(description = "Category name", example = "Electronics")
    private String name;

    @Schema(description = "Number of products in category", example = "25")
    private Long productCount;

    @Schema(description = "Minimum price in category", example = "99.99")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price in category", example = "1999.99")
    private BigDecimal maxPrice;

    @Schema(description = "Average price in category", example = "549.99")
    private Double avgPrice;
}