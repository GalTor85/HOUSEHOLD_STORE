package ru.galtor85.household_store.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.order.SalesOrderType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sales order create request")
public class SalesOrderCreateRequest {

    @NotNull(message = "{sales.validation.user.id.empty}")
    @Schema(description = "User ID", example = "1")
    private Long userId;

    @NotEmpty(message = "{sales.validation.items.empty}")
    @Valid
    @Schema(description = "Items to purchase")
    private List<SalesOrderItemCreateDto> items;

    @NotNull(message = "{sales.validation.order.type.empty}")
    @Schema(description = "Order type", example = "RETAIL", required = true)
    private SalesOrderType orderType;

    @Schema(description = "Shipping address", example = "123 Main St, Moscow")
    private String shippingAddress;

    @Schema(description = "Billing address", example = "123 Main St, Moscow")
    private String billingAddress;

    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Discount amount to apply to the order", example = "100.00")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Schema(description = "Shipping cost", example = "50.00")
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Schema(description = "Tax amount", example = "0.00")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Schema(description = "Notes")
    private String notes;

    /**
     * Checks if the order has a discount applied.
     *
     * @return true if discount amount is positive
     */
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the order has shipping cost.
     *
     * @return true if shipping amount is positive
     */
    public boolean hasShipping() {
        return shippingAmount != null && shippingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if the order has tax.
     *
     * @return true if tax amount is positive
     */
    public boolean hasTax() {
        return taxAmount != null && taxAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Gets effective discount amount (never null).
     *
     * @return discount amount or zero
     */
    public BigDecimal getEffectiveDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    /**
     * Gets effective shipping amount (never null).
     *
     * @return shipping amount or zero
     */
    public BigDecimal getEffectiveShippingAmount() {
        return shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
    }

    /**
     * Gets effective tax amount (never null).
     *
     * @return tax amount or zero
     */
    public BigDecimal getEffectiveTaxAmount() {
        return taxAmount != null ? taxAmount : BigDecimal.ZERO;
    }
}