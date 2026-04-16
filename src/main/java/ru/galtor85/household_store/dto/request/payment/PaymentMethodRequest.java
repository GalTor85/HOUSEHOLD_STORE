package ru.galtor85.household_store.dto.request.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for payment method selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment method request")
public class PaymentMethodRequest {

    @NotNull(message = "{payment.validation.payment.method.id.required}")
    @Schema(description = "Payment method ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentMethodId;

    @Schema(description = "Payment amount (pays remaining if not specified)", example = "1500.00")
    private BigDecimal amount;

    @JsonIgnore
    public boolean hasAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}