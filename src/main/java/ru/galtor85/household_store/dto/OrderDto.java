package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Order DTO", title = "Order")
public class OrderDto {

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Unique order number", example = "ORD-20240101-001")
    private String orderNumber;

    @Schema(description = "User ID (for retail/wholesale orders)", example = "1")
    private Long userId;

    @Schema(description = "Supplier ID (for purchase orders)", example = "1")
    private Long supplierId;

    @Schema(description = "Order type", example = "RETAIL")
    private OrderType orderType;

    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Localized order status", example = "Ожидает оплаты")
    private String localizedStatus;

    @Schema(description = "Order items")
    private List<OrderItemDto> items;

    @Schema(description = "Subtotal before discounts", example = "1000.00")
    private BigDecimal subtotal;

    @Schema(description = "Total discount amount", example = "100.00")
    private BigDecimal discountAmount;

    @Schema(description = "Final total amount", example = "950.00")
    private BigDecimal totalAmount;

    @Schema(description = "Shipping cost", example = "50.00")
    private BigDecimal shippingAmount;

    @Schema(description = "Tax amount", example = "0.00")
    private BigDecimal taxAmount;

    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Payment details (transaction ID)", example = "txn_123456")
    private String paymentDetails;

    @Schema(description = "Shipping address", example = "123 Main St, Moscow")
    private String shippingAddress;

    @Schema(description = "Billing address", example = "123 Main St, Moscow")
    private String billingAddress;

    @Schema(description = "Tracking number", example = "RU123456789")
    private String trackingNumber;

    @Schema(description = "Estimated delivery date")
    private LocalDateTime estimatedDelivery;

    @Schema(description = "Actual delivery date")
    private LocalDateTime deliveredAt;

    @Schema(description = "Cancellation date")
    private LocalDateTime cancelledAt;

    @Schema(description = "Cancellation reason", example = "Out of stock")
    private String cancellationReason;

    @Schema(description = "Created by user/manager ID", example = "1")
    private Long createdBy;

    @Schema(description = "Order notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    // ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ ДЛЯ УДОБСТВА
    @Schema(description = "Customer name (for display)", example = "Иван Иванов")
    private String customerName;

    @Schema(description = "Customer email", example = "ivan@example.com")
    private String customerEmail;

    @Schema(description = "Supplier name", example = "ООО 'ТехноПост'")
    private String supplierName;

    @Schema(description = "Item count", example = "3")
    private Integer itemCount;
}