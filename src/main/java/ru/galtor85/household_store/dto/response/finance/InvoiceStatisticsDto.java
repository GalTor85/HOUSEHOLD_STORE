package ru.galtor85.household_store.dto.response.finance;

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
@Schema(description = "Invoice statistics DTO")
public class InvoiceStatisticsDto {

    @Schema(description = "Total pending amount for purchase orders", example = "44600.00")
    private BigDecimal purchasePendingTotal;

    @Schema(description = "Total pending amount for sales orders", example = "0.00")
    private BigDecimal salesPendingTotal;

    @Schema(description = "Total pending amount for all invoices", example = "44600.00")
    private BigDecimal totalPending;

    @Schema(description = "Localized total pending amount", example = "Остаток к оплате: 44 600.00 ₽")
    private String localizedTotalPending;

    @Schema(description = "Localized purchase pending amount", example = "Закупки: 44 600.00 ₽")
    private String localizedPurchasePending;

    @Schema(description = "Localized sales pending amount", example = "Продажи: 0.00 ₽")
    private String localizedSalesPending;
}