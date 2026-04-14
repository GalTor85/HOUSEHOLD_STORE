package ru.galtor85.household_store.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for sales order information.
 *
 * <p>Contains complete order details including items, pricing, delivery,
 * payment, and reservation information for cash payments.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sales order DTO")
public class SalesOrderDto {

    // =========================================================================
    // IDENTIFIERS
    // =========================================================================

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Unique order number", example = "SO-20240101-001")
    private String orderNumber;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    // =========================================================================
    // ORDER TYPE & STATUS
    // =========================================================================

    @Schema(description = "Order type", example = "RETAIL")
    private SalesOrderType orderType;

    @Schema(description = "Localized order type", example = "Розничный заказ")
    private String localizedOrderType;

    @Schema(description = "Order status")
    private OrderStatus status;

    @Schema(description = "Localized order status", example = "Ожидает оплаты")
    private String localizedStatus;

    // =========================================================================
    // ORDER ITEMS
    // =========================================================================

    @Schema(description = "Order items")
    private List<SalesOrderItemDto> items;

    // =========================================================================
    // FINANCIAL FIELDS
    // =========================================================================

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

    // =========================================================================
    // PAYMENT FIELDS
    // =========================================================================

    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Payment details (transaction ID)", example = "txn_123456")
    private String paymentDetails;

    @Schema(description = "Payment summary for the order")
    private PaymentSummaryDto paymentSummary;

    // =========================================================================
    // DELIVERY FIELDS
    // =========================================================================

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

    // =========================================================================
    // CANCELLATION FIELDS
    // =========================================================================

    @Schema(description = "Cancellation date")
    private LocalDateTime cancelledAt;

    @Schema(description = "Cancellation reason", example = "Out of stock")
    private String cancellationReason;

    // =========================================================================
    // RESERVATION FIELDS (for cash payments)
    // =========================================================================

    @Schema(description = "Reservation status", example = "ACTIVE")
    private String reservationStatus;

    @Schema(description = "Reserved until", example = "2026-04-19T15:52:42")
    private LocalDateTime reservedUntil;

    @Schema(description = "Localized reservation status", example = "Товар зарезервирован до 19.04.2026")
    private String localizedReservationStatus;

    // =========================================================================
    // AUDIT FIELDS
    // =========================================================================

    @Schema(description = "Created by user/manager ID", example = "1")
    private Long createdBy;

    @Schema(description = "Order notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    // =========================================================================
    // EXTENDED FIELDS (for convenience)
    // =========================================================================

    @Schema(description = "Customer name", example = "Иван Иванов")
    private String customerName;

    @Schema(description = "Customer email", example = "ivan@example.com")
    private String customerEmail;

    @Schema(description = "Item count", example = "3")
    private Integer itemCount;
}