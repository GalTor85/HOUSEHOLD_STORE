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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.TechnicalConstants.DATE_FORMAT_PATTERN;

/**
 * Converter for SalesOrder entity to DTO.
 *
 * <p>Handles conversion of sales order entities to various DTO formats
 * including basic order info, items, and extended details with customer information.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderConverter {

    private final MessageService messageService;

    /**
     * Converts SalesOrder entity to DTO.
     *
     * @param order the sales order entity
     * @return sales order DTO
     */
    public SalesOrderDto toDto(SalesOrder order) {
        if (order == null) {
            return null;
        }

        List<SalesOrderItemDto> itemDtos = toItemDtoList(order.getItems());

        String localizedStatus = messageService.get(
                "order.status." + order.getStatus().name()
        );

        String localizedReservationStatus = buildLocalizedReservationStatus(order);

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
                .reservationStatus(order.getReservationStatus() != null ? order.getReservationStatus().name() : null)
                .reservedUntil(order.getReservedUntil())
                .localizedReservationStatus(localizedReservationStatus)
                .build();
    }

    /**
     * Converts SalesOrderItem entity to DTO.
     *
     * @param item the order item entity
     * @return order item DTO
     */
    public SalesOrderItemDto toItemDto(SalesOrderItem item) {
        if (item == null) {
            return null;
        }

        BigDecimal totalPrice = calculateTotalPrice(item);
        BigDecimal discountedPrice = calculateDiscountedPrice(item, totalPrice);

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
     * Converts list of SalesOrderItem entities to DTO list.
     *
     * @param items list of order items
     * @return list of order item DTOs
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
     * Converts SalesOrder entity to DTO with customer details.
     *
     * @param order the sales order entity
     * @param customerName customer full name
     * @param customerEmail customer email
     * @return sales order DTO with customer info
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
     * Converts SalesOrder entity to DTO with statistics.
     *
     * @param order the sales order entity
     * @param totalItemsCount total number of items in order
     * @param totalDiscount total discount amount
     * @return sales order DTO with statistics
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

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Builds localized reservation status message.
     *
     * @param order the sales order entity
     * @return localized reservation status or null
     */
    private String buildLocalizedReservationStatus(SalesOrder order) {
        if (order.getReservationStatus() == null) {
            return null;
        }

        if (order.getReservationStatus() == SalesOrder.ReservationStatus.ACTIVE
                && order.getReservedUntil() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
            return messageService.get("order.reserved.until",
                    order.getReservedUntil().format(formatter));
        }

        return messageService.get("order.reservation.status." + order.getReservationStatus().name());
    }

    /**
     * Calculates total price for an order item.
     *
     * @param item the order item
     * @return total price (price × quantity)
     */
    private BigDecimal calculateTotalPrice(SalesOrderItem item) {
        if (item.getPrice() == null || item.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    /**
     * Calculates discounted price for an order item.
     *
     * @param item the order item
     * @param totalPrice the total price
     * @return discounted price or null if no discount
     */
    private BigDecimal calculateDiscountedPrice(SalesOrderItem item, BigDecimal totalPrice) {
        if (item.getDiscountAmount() == null) {
            return null;
        }
        return totalPrice.subtract(item.getDiscountAmount());
    }
}