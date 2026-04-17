package ru.galtor85.household_store.dto.response.supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Supplier statistics DTO")
public class SupplierStatisticsDto {

    @Schema(description = "Supplier ID", example = "1")
    private Long supplierId;

    @Schema(description = "Supplier name", example = "TechnoPost LLC")
    private String supplierName;

    @Schema(description = "Number of purchase orders", example = "15")
    private Long orderCount;

    @Schema(description = "Number of products", example = "25")
    private Long productCount;

    @Schema(description = "Total purchased amount", example = "150000.00")
    private BigDecimal totalPurchased;

    @Schema(description = "Last order date", example = "2026-04-15T10:30:00")
    private LocalDateTime lastOrderDate;
}