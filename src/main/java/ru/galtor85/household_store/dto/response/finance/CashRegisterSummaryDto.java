package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cash register summary DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cash register summary for a period")
public class CashRegisterSummaryDto {

    @Schema(description = "Cash register ID", example = "1")
    private Long cashRegisterId;

    @Schema(description = "Cash register name", example = "Main Cash Register")
    private String cashRegisterName;

    @Schema(description = "Period start")
    private LocalDateTime startDate;

    @Schema(description = "Period end")
    private LocalDateTime endDate;

    @Schema(description = "Opening balance", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Closing balance", example = "15000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Total income", example = "5000.00")
    private BigDecimal totalIncome;

    @Schema(description = "Total expense", example = "2000.00")
    private BigDecimal totalExpense;

    @Schema(description = "Net turnover (income - expense)", example = "3000.00")
    private BigDecimal netTurnover;

    @Schema(description = "Number of transactions", example = "15")
    private Integer transactionCount;
}