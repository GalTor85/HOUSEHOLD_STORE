package ru.galtor85.household_store.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;
import ru.galtor85.household_store.entity.user.UserType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for payment method with user type assignments.
 *
 * <p>Used for manager view to display payment methods with their
 * associated user types and additional metadata.</p>
 *
 * @author G@LTor85
 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment method DTO with user type assignments")
public class PaymentMethodWithUserTypesDto {

    @Schema(description = "Payment method ID", example = "1")
    private Long id;

    @Schema(description = "Payment method name", example = "Sberbank Card")
    private String name;

    @Schema(description = "Payment method type", example = "CREDIT_CARD")
    private PaymentMethodType methodType;

    @Schema(description = "Masked identifier", example = "**** **** **** 1234")
    private String maskedIdentifier;

    @Schema(description = "Payment provider", example = "SBERBANK")
    private PaymentProvider provider;

    @Schema(description = "Is active", example = "true")
    private Boolean active;

    @Schema(description = "Localized status", example = "Active")
    private String localizedStatus;

    @Schema(description = "Currency", example = "RUB")
    private String currency;

    @Schema(description = "Processing fee percentage", example = "0.00")
    private BigDecimal processingFee;

    @Schema(description = "User types that can use this payment method")
    private Set<UserType> availableForUserTypes;

    @Schema(description = "Sort order", example = "0")
    private Integer sortOrder;

    @Schema(description = "Created by", example = "1")
    private Long createdBy;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated at")
    private LocalDateTime updatedAt;
}