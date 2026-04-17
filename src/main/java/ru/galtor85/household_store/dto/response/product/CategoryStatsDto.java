// CategoryStatsDto.java
package ru.galtor85.household_store.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDto {
    private String name;
    private Long productCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double avgPrice;
}