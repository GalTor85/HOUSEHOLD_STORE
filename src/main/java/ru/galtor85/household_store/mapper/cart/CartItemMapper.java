package ru.galtor85.household_store.mapper.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.entity.cart.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper for cart item entities to DTOs.
 */
@SuppressWarnings("unused")
@Slf4j
@Component
@RequiredArgsConstructor
public class CartItemMapper {

    /**
     * Converts cart item entity to DTO.
     *
     * @param item cart item entity
     * @return cart item DTO
     */
    public CartItemDto toDto(CartItem item) {
        if (item == null) {
            return null;
        }
        return CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    /**
     * Converts list of cart item entities to list of DTOs.
     *
     * @param items list of cart item entities
     * @return list of cart item DTOs
     */
    @SuppressWarnings("unused")
    public List<CartItemDto> toDtoList(List<CartItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::toDto)
                .toList();
    }
}