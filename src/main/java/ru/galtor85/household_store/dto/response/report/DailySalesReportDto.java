package ru.galtor85.household_store.dto.response.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Daily sales report DTO")
public class DailySalesReportDto {

    @Schema(description = "Report date", example = "2026-04-17")
    private LocalDate date;

    @Schema(description = "Total orders count", example = "25")
    private Long totalOrders;

    @Schema(description = "Completed orders", example = "20")
    private Long completedOrders;

    @Schema(description = "Cancelled orders", example = "2")
    private Long cancelledOrders;

    @Schema(description = "Pending orders", example = "3")
    private Long pendingOrders;

    @Schema(description = "Total sales amount", example = "15000.00")
    private BigDecimal totalSalesAmount;

    @Schema(description = "Average order value", example = "600.00")
    private BigDecimal averageOrderValue;

    @Schema(description = "Min order amount", example = "50.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Max order amount", example = "5000.00")
    private BigDecimal maxOrderAmount;

    @Schema(description = "Unique customers", example = "18")
    private Long uniqueCustomers;

    @Schema(description = "Top products")
    private List<TopProductDto> topProducts;

    @Schema(description = "Sales by hour")
    private List<HourlySalesDto> salesByHour;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Top product DTO")
    public static class TopProductDto {
        private Long productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hourly sales DTO")
    public static class HourlySalesDto {
        private Integer hour;
        private Long ordersCount;
        private BigDecimal amount;
    }
}