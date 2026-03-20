package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.entity.CartItem;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartItemMapper {

    /**
     * Преобразование сущности в DTO
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
                .totalPrice(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    /**
     * Преобразование списка сущностей в список DTO
     */
    public List<CartItemDto> toDtoList(List<CartItem> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}