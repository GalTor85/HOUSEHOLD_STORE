package ru.galtor85.household_store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationRequest {
    private Long userId;
    private List<CartItemDto> items;
    private String promoCode;
    private Long userTypeId;
}