package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.OrderDto;
import ru.galtor85.household_store.dto.OrderItemDto;
import ru.galtor85.household_store.entity.Order;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final MessageService messageService;
    private final OrderItemMapper orderItemMapper;

    /**
     * Преобразование сущности в DTO
     */
    public OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemDto> itemDtos = order.getItems() != null ?
                orderItemMapper.toDtoList(order.getItems()) : null;

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
                .paymentDetails(order.getPaymentDetails())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .createdBy(order.getCreatedBy())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}