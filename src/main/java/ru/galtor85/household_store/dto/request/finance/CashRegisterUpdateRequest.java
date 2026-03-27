package ru.galtor85.household_store.dto.request.finance;

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
@Schema(description = "Cash register update request")
public class CashRegisterUpdateRequest {

    @Schema(description = "Cash register name", example = "Основная касса")
    private String name;

    @Schema(description = "Location", example = "Главный зал")
    private String location;

    @Schema(description = "Opening balance", example = "10000.00")
    private BigDecimal openingBalance;
}