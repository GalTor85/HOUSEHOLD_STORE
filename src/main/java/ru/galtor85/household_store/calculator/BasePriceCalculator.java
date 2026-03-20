package ru.galtor85.household_store.calculator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CartItemDto;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BasePriceCalculator {

    public BigDecimal calculateOriginalTotal(List<CartItemDto> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}