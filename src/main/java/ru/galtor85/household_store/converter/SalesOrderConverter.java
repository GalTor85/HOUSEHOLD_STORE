package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderItemDto;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderConverter {

    private final MessageService messageService;

    /**
     * Конвертирует сущность заказа в DTO
     */
    public SalesOrderDto toDto(SalesOrder order) {
        if (order == null) {
            return null;
        }

        List<SalesOrderItemDto> itemDtos = toItemDtoList(order.getItems());

        String localizedStatus = messageService.get(
                "order.status." + order.getStatus().name()
        );

        return SalesOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
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

    /**
     * Конвертирует сущность позиции в DTO
     */
    public SalesOrderItemDto toItemDto(SalesOrderItem item) {
        if (item == null) {
            return null;
        }

        BigDecimal totalPrice = item.getPrice() != null ?
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())) :
                BigDecimal.ZERO;

        BigDecimal discountedPrice = null;
        if (item.getDiscountAmount() != null) {
            discountedPrice = totalPrice.subtract(item.getDiscountAmount());
        }

        return SalesOrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(totalPrice)
                .discountAmount(item.getDiscountAmount())
                .discountedPrice(discountedPrice)
                .notes(item.getNotes())
                .build();
    }

    /**
     * Конвертирует список позиций в список DTO
     */
    public List<SalesOrderItemDto> toItemDtoList(List<SalesOrderItem> items) {
        if (items == null) {
            return null;
        }

        return items.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность заказа в DTO с дополнительной информацией
     */
    public SalesOrderDto toDtoWithDetails(SalesOrder order,
                                          String customerName,
                                          String customerEmail) {
        SalesOrderDto dto = toDto(order);

        if (dto != null) {
            dto.setCustomerName(customerName);
            dto.setCustomerEmail(customerEmail);
            dto.setItemCount(order.getItems() != null ? order.getItems().size() : 0);
        }

        return dto;
    }

    /**
     * Конвертирует сущность заказа в DTO со статистикой
     */
    public SalesOrderDto toDtoWithStatistics(SalesOrder order,
                                             Integer totalItemsCount,
                                             BigDecimal totalDiscount) {
        SalesOrderDto dto = toDto(order);

        if (dto != null) {
            dto.setItemCount(totalItemsCount);
            if (totalDiscount != null) {
                dto.setDiscountAmount(totalDiscount);
            }
        }

        return dto;
    }
}