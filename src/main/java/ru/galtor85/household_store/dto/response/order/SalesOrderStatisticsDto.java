package ru.galtor85.household_store.dto.response.order;

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
@Schema(description = "SalesOrder statistics DTO", title = "SalesOrder Statistics")
public class SalesOrderStatisticsDto {

    @Schema(description = "Total orders today", example = "15")
    private long totalOrdersToday;

    @Schema(description = "Total orders this week", example = "87")
    private long totalOrdersWeek;

    @Schema(description = "Total orders this month", example = "345")
    private long totalOrdersMonth;

    @Schema(description = "Revenue today", example = "15000.00")
    private BigDecimal revenueToday;

    @Schema(description = "Revenue this week", example = "87500.00")
    private BigDecimal revenueWeek;

    @Schema(description = "Revenue this month", example = "345000.00")
    private BigDecimal revenueMonth;

    // Дополнительные поля для аналитики
    @Schema(description = "Average salesOrder value today", example = "1000.00")
    private BigDecimal averageOrderValueToday;

    @Schema(description = "Average salesOrder value this week", example = "1005.75")
    private BigDecimal averageOrderValueWeek;

    @Schema(description = "Average salesOrder value this month", example = "1000.00")
    private BigDecimal averageOrderValueMonth;

    @Schema(description = "Most popular product category", example = "Electronics")
    private String mostPopularCategory;

    @Schema(description = "Most popular product", example = "iPhone 13 Pro")
    private String mostPopularProduct;
}