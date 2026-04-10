package ru.galtor85.household_store.dto.response.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.payment.PaymentMethodType;
import ru.galtor85.household_store.entity.payment.PaymentProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment method response DTO")
public class PaymentMethodDto {

    @Schema(description = "Payment method ID", example = "1")
    private Long id;

    @Schema(description = "Payment method name", example = "My Sberbank Card")
    private String name;

    @Schema(description = "Payment method type", example = "CREDIT_CARD")
    private PaymentMethodType methodType;

    @Schema(description = "Masked identifier", example = "**** **** **** 1234")
    private String maskedIdentifier;

    @Schema(description = "Payment provider", example = "SBERBANK")
    private PaymentProvider provider;

    @Schema(description = "Is active", example = "true")
    private Boolean active;

    @Schema(description = "Is default", example = "false")
    private Boolean isDefault;

    @Schema(description = "Currency", example = "RUB")
    private String currency;

    @Schema(description = "Processing fee percentage", example = "1.5")
    private BigDecimal processingFee;

    @Schema(description = "Created by user ID", example = "1")
    private Long createdBy;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Localized status", example = "Active")
    private String localizedStatus;
}