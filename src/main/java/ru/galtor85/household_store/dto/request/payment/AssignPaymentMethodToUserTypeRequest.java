package ru.galtor85.household_store.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.user.UserType;

import java.util.Set;

/**
 * Request DTO for assigning payment methods to user types.
 *
 * @author G@LTor85
 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign payment methods to user types")
public class AssignPaymentMethodToUserTypeRequest {

    @NotNull(message = "{payment.validation.payment.method.id.required}")
    @Schema(description = "Payment method ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long paymentMethodId;

    @NotNull(message = "{payment.validation.user.type.required}")
    @Schema(description = "User types that can use this payment method",
            example = "[\"RETAIL\", \"WHOLESALE\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<UserType> userTypes;

    @Schema(description = "Sort order for display", example = "0")
    private Integer sortOrder;
}