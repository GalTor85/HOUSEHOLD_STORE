package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.OrderItemDto;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderItemMapper {

    private final MessageService messageService;

    /**
     * Преобразование сущности в DTO
     */
    public OrderItemDto toDto(OrderItem item, Locale locale) {
        if (item == null) {
            return null;
        }

        return OrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .supplierId(item.getSupplierProductId())
                .supplierSku(item.getSupplierSku())
                .notes(null) // Можно добавить, если нужно
                .build();
    }

    /**
     * Преобразование списка сущностей в список DTO
     */
    public List<OrderItemDto> toDtoList(List<OrderItem> items, Locale locale) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(item -> toDto(item, locale))
                .collect(Collectors.toList());
    }
}