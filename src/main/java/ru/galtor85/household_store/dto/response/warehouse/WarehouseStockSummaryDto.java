package ru.galtor85.household_store.dto.response.warehouse;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.response.stock.TopProductDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Warehouse stock summary", title = "Warehouse Stock Summary")
public class WarehouseStockSummaryDto {

    @Schema(description = "Warehouse ID", example = "1")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main Warehouse")
    private String warehouseName;

    @Schema(description = "Total products", example = "150")
    private Integer totalProducts;

    @Schema(description = "Total items in stock", example = "1250")
    private Integer totalItems;

    @Schema(description = "Total stock value", example = "1250000.00")
    private Double totalValue;

    @Schema(description = "Low stock items count", example = "5")
    private Integer lowStockCount;

    @Schema(description = "Out of stock items", example = "2")
    private Integer outOfStockCount;

    @Schema(description = "Warehouse utilization", example = "75.5")
    private Double utilizationPercentage;

    @Schema(description = "Top 5 products by value")
    private List<TopProductDto> topProducts;
}