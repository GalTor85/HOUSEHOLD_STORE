package ru.galtor85.household_store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.OrderStatus;
import ru.galtor85.household_store.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String orderNumber;
    private Long userId;
    private Long supplierId;
    private OrderType orderType;
    private OrderStatus status;
    private String localizedStatus;
    private List<OrderItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private String paymentMethod;
    private String shippingAddress;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;
}