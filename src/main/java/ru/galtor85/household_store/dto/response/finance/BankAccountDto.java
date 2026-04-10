package ru.galtor85.household_store.dto.response.finance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.finance.BankAccountType;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Bank account response DTO", title = "Bank Account Response")
public class BankAccountDto {

    @Schema(description = "Account ID", example = "1")
    private Long id;

    @Schema(description = "Account number", example = "40702810123456789012")
    private String accountNumber;

    @Schema(description = "Account name", example = "Main Operating Account")
    private String name;

    @Schema(description = "Bank name", example = "Sberbank")
    private String bankName;

    @Schema(description = "BIC", example = "044525225")
    private String bic;

    @Schema(description = "Correspondent account", example = "30101810400000000225")
    private String correspondentAccount;

    @Schema(description = "IBAN", example = "RU12345678901234567890")
    private String iban;

    @Schema(description = "SWIFT code", example = "SABRRUMM")
    private String swiftCode;

    @Schema(description = "Current balance", example = "1500000.00")
    private BigDecimal balance;

    @Schema(description = "Currency", example = "RUB")
    private String currency;

    @Schema(description = "Account type", example = "CHECKING")
    private BankAccountType accountType;

    @Schema(description = "Localized account type", example = "Current account")
    private String localizedAccountType;

    @Schema(description = "Is active", example = "true")
    private Boolean active;

    @Schema(description = "Localized status", example = "Active")
    private String localizedStatus;

    @Schema(description = "Created by user ID", example = "1")
    private Long createdBy;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Display name", example = "Main Operating Account (40702810123456789012)")
    private String displayName;

    @Schema(description = "Formatted balance", example = "1,500,000.00 RUB")
    private String formattedBalance;

    @Schema(description = "Localized balance", example = "Balance: 1 500 000.00 ₽")
    private String localizedBalance;

    /**
     * Checks if account is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /**
     * Gets formatted balance with currency.
     *
     * @return formatted balance string
     */
    public String getFormattedBalance() {
        return String.format("%,.2f %s", balance, currency);
    }

    /**
     * Initializes localized fields for UI display.
     *
     * @param messageService message service for localization
     */
    public void initLocalizedFields(MessageService messageService, String currency) {
        if (accountType != null) {
            localizedAccountType = accountType.getLocalizedName(messageService);
        }

        if (active != null) {
            localizedStatus = active ?
                    messageService.get("bank.account.status.active") :
                    messageService.get("bank.account.status.inactive");
        }

        if (balance != null && currency != null) {
            localizedBalance = messageService.get("bank.account.balance",
                    String.format("%,.2f", balance), currency);
        }

        displayName = name + " (" + accountNumber + ")";
        formattedBalance = getFormattedBalance();
    }
}