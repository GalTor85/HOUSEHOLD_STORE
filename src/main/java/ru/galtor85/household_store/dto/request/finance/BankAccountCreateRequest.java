package ru.galtor85.household_store.dto.request.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.BankAccountType;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for creating a bank account.
 * Contains all necessary fields for bank account creation with validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a bank account", title = "Bank Account Create Request")
public class BankAccountCreateRequest {

    @NotBlank(message = "{bank.account.validation.account.number.empty}")
    @Pattern(regexp = BANK_ACCOUNT_NUMBER_PATTERN, message = "{bank.account.validation.account.number.pattern}")
    @Schema(description = "Bank account number", example = "40702810123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @NotBlank(message = "{bank.account.validation.name.empty}")
    @Schema(description = "Account display name", example = "Main Operating Account")
    private String name;

    @NotBlank(message = "{bank.account.validation.bank.name.empty}")
    @Schema(description = "Bank name", example = "Sberbank")
    private String bankName;

    @Pattern(regexp = BIC_PATTERN, message = "{bank.account.validation.bic.pattern}")
    @Schema(description = "Bank BIC", example = "044525225")
    private String bic;

    @Pattern(regexp = CORRESPONDENT_ACCOUNT_PATTERN, message = "{bank.account.validation.correspondent.account.pattern}")
    @Schema(description = "Correspondent account", example = "30101810400000000225")
    private String correspondentAccount;

    @Schema(description = "IBAN", example = "RU12345678901234567890")
    private String iban;

    @Schema(description = "SWIFT code", example = "SABRRUMM")
    private String swiftCode;

    @NotNull(message = "{bank.account.validation.account.type.empty}")
    @Schema(description = "Account type", example = "CHECKING", requiredMode = Schema.RequiredMode.REQUIRED)
    private BankAccountType accountType;

    @Schema(description = "Initial balance", example = "0.00", defaultValue = "0.00")
    @Builder.Default
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Schema(description = "Currency code", example = "RUB")
    private String currency;

    @Schema(description = "Is active", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean active = DEFAULT_ACTIVE_STATUS;
}