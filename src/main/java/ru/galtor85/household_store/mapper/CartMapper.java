package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.CartDto;
import ru.galtor85.household_store.dto.CartItemDto;
import ru.galtor85.household_store.entity.Cart;
import ru.galtor85.household_store.entity.CartItem;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMapper {

    private final MessageService messageService;
    private final CartItemMapper cartItemMapper;

    /**
     * Преобразование сущности в DTO
     */
    public CartDto toDto(Cart cart, Locale locale) {
        if (cart == null) {
            return null;
        }

        List<CartItemDto> itemDtos = cart.getItems() != null ?
                cartItemMapper.toDtoList(cart.getItems(), locale) : null;

        String localizedStatus = messageService.get("cart.status." + cart.getStatus().name());

        return CartDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .localizedStatus(localizedStatus)
                .items(itemDtos)
                .totalAmount(cart.getTotalAmount())
                .itemsCount(cart.getItemsCount())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }
}