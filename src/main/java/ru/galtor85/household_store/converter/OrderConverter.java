package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.dto.OrderItemDto;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.entity.OrderItem;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderConverter {

    private final MessageService messageService;

    public OrderDto convertToDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());

        String localizedStatus = messageService.get("order.status." + order.getStatus().name());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .supplierId(order.getSupplierId())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .localizedStatus(localizedStatus)
                .items(itemDtos)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .createdAt(order.getCreatedAt())
                .notes(order.getNotes())
                .build();
    }

    public OrderItemDto convertItemToDto(OrderItem item) {
        return OrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}