package ru.galtor85.household_store.calculator;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calculator for base price without discounts.
 */
@Component
public class BasePriceCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Calculates original total from cart items.
     *
     * @param items cart items
     * @return sum of price * quantity for all items
     */
    public BigDecimal calculateOriginalTotal(List<CartItemDto> items) {
        if (items == null || items.isEmpty()) {
            return ZERO;
        }
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add);
    }
}