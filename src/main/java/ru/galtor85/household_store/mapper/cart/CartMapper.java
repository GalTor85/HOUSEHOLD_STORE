package ru.galtor85.household_store.mapper.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.cart.CartDto;
import ru.galtor85.household_store.dto.response.cart.CartItemDto;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartItem;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMapper {

    private final MessageService messageService;
    private final CartItemMapper cartItemMapper;

    /**
     * Преобразование сущности в DTO
     */
    public CartDto toDto(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemDto> itemDtos = cart.getItems() != null ?
                cartItemMapper.toDtoList(cart.getItems()) : null;

        String localizedStatus = messageService.get("cart.status." + cart.getStatus().name());

        String localizedCategory = determineCartCategory(cart);

        return CartDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .localizedStatus(localizedStatus)
                .items(itemDtos)
                .category(localizedCategory)
                .totalAmount(cart.getTotalAmount())
                .itemsCount(cart.getItemsCount())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }

    private String determineCartCategory(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getCategory)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}