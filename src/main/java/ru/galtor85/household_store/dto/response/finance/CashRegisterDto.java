package ru.galtor85.household_store.dto.response.finance;

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
@Schema(description = "Cash register DTO")
public class CashRegisterDto {


    @Schema(description = "Cash register ID", example = "1")
    private Long id;

    @Schema(description = "Cash register number", example = "REG-001")
    private String registerNumber;

    @Schema(description = "Cash register name", example = "Основная касса")
    private String name;

    @Schema(description = "Location", example = "Главный зал")
    private String location;

    @Schema(description = "Is cash register active", example = "true")
    private Boolean isActive;

    @Schema(description = "Localized status", example = "Активна")
    private String localizedStatus;

    @Schema(description = "Opening balance", example = "10000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Closing balance", example = "15000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Current balance (for active cash registers)", example = "12500.00")
    private BigDecimal currentBalance;

    @Schema(description = "Discrepancy", example = "100.00")
    private BigDecimal discrepancy;

    @Schema(description = "Cashier ID", example = "1")
    private Long cashierId;

    @Schema(description = "Opened at timestamp")
    private LocalDateTime openedAt;

    @Schema(description = "Closed at timestamp")
    private LocalDateTime closedAt;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at timestamp")
    private LocalDateTime updatedAt;
}