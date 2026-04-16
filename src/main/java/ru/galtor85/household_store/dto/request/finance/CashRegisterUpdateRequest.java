package ru.galtor85.household_store.dto.request.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a cash register.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash register update request")
public class CashRegisterUpdateRequest {

    @Schema(description = "Cash register name", example = "Main Cash Register")
    private String name;

    @Schema(description = "Location", example = "Main hall, counter #1")
    private String location;
}