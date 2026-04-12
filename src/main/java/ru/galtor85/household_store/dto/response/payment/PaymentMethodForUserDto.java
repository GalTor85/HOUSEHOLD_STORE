package ru.galtor85.household_store.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;

/**
 * DTO for payment method visible to customers.
 * Contains only non-sensitive information.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment method DTO for customers (safe view)")
public class PaymentMethodForUserDto {

    @Schema(description = "Payment method ID", example = "1")
    private Long id;

    @Schema(description = "Payment method name", example = "Sberbank Card")
    private String name;

    @Schema(description = "Payment method type", example = "CREDIT_CARD")
    private PaymentMethodType methodType;

    @Schema(description = "Masked identifier (safe for display)", example = "**** **** **** 1234")
    private String maskedIdentifier;

    @Schema(description = "Payment provider", example = "SBERBANK")
    private PaymentProvider provider;

    @Schema(description = "Currency", example = "RUB")
    private String currency;

    @Schema(description = "Sort order for display", example = "0")
    private Integer sortOrder;
}