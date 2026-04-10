package ru.galtor85.household_store.dto.request.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.entity.order.SalesOrderType;

import java.math.BigDecimal;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

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
    @Schema(description = "Order type", example = "RETAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private SalesOrderType orderType;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{sales.validation.shipping.address.max}")
    @Schema(description = "Shipping address", example = "123 Main St, Moscow")
    private String shippingAddress;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{sales.validation.billing.address.max}")
    @Schema(description = "Billing address", example = "123 Main St, Moscow")
    private String billingAddress;

    @Size(max = MAX_PAYMENT_METHOD_LENGTH, message = "{sales.validation.payment.method.max}")
    @Schema(description = "Payment method", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Discount amount to apply to the order", example = "100.00")
    private BigDecimal discountAmount;

    @Schema(description = "Shipping cost", example = "50.00")
    private BigDecimal shippingAmount;

    @Schema(description = "Tax amount", example = "0.00")
    private BigDecimal taxAmount;

    @Size(max = MAX_NOTES_LENGTH, message = "{sales.validation.notes.max}")
    @Schema(description = "Notes")
    private String notes;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDiscount() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasShipping() {
        return shippingAmount != null && shippingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasTax() {
        return taxAmount != null && taxAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public BigDecimal getEffectiveDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public BigDecimal getEffectiveShippingAmount() {
        return shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public BigDecimal getEffectiveTaxAmount() {
        return taxAmount != null ? taxAmount : BigDecimal.ZERO;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasShippingAddress() {
        return shippingAddress != null && !shippingAddress.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBillingAddress() {
        return billingAddress != null && !billingAddress.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPaymentMethod() {
        return paymentMethod != null && !paymentMethod.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }
}