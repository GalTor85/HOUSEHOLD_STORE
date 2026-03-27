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

    @Schema(description = "Notes")
    private String notes;
}