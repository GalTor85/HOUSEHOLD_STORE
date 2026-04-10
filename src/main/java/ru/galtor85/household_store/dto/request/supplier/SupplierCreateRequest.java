package ru.galtor85.household_store.dto.request.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for creating a supplier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier create request DTO", title = "Supplier Create Request")
public class SupplierCreateRequest {

    // =========================================================================
    // BASIC INFORMATION
    // =========================================================================

    @NotBlank(message = "{supplier.validation.name.empty}")
    @Size(min = MIN_SUPPLIER_NAME_LENGTH, max = MAX_SUPPLIER_NAME_LENGTH, message = "{supplier.validation.name.size}")
    @Schema(description = "Company name", example = "TechnoPost LLC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Email(message = "{supplier.validation.email.invalid}")
    @Size(max = MAX_EMAIL_LENGTH, message = "{supplier.validation.email.size}")
    @Schema(description = "Contact email", example = "info@technopost.ru")
    private String email;

    @Pattern(regexp = PHONE_PATTERN, message = "{supplier.validation.phone.invalid}")
    @Schema(description = "Contact phone", example = "+74951234567")
    private String phone;

    @Size(max = MAX_WEBSITE_LENGTH, message = "{supplier.validation.website.max}")
    @Schema(description = "Website", example = "https://www.technopost.ru")
    private String website;

    @Size(max = MAX_CONTACT_PERSON_LENGTH, message = "{supplier.validation.contact.person.size}")
    @Schema(description = "Contact person", example = "Ivan Ivanov")
    private String contactPerson;

    // =========================================================================
    // LEGAL INFORMATION
    // =========================================================================

    @Pattern(regexp = INN_PATTERN, message = "{supplier.validation.inn.invalid}")
    @Schema(description = "INN (10 or 12 digits for Russian companies)", example = "7701234567")
    private String inn;

    @Pattern(regexp = KPP_PATTERN, message = "{supplier.validation.kpp.invalid}")
    @Schema(description = "KPP (9 digits for Russian companies)", example = "770101001")
    private String kpp;

    @Pattern(regexp = OGRN_PATTERN, message = "{supplier.validation.ogrn.invalid}")
    @Schema(description = "OGRN (13 digits for Russian companies)", example = "1027701234567")
    private String ogrn;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{supplier.validation.legal.address.size}")
    @Schema(description = "Legal address", example = "123 Legal St, Moscow")
    private String legalAddress;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{supplier.validation.actual.address.size}")
    @Schema(description = "Actual address", example = "123 Actual St, Moscow")
    private String actualAddress;

    // =========================================================================
    // BANK DETAILS
    // =========================================================================

    @Size(max = MAX_BANK_NAME_LENGTH, message = "{supplier.validation.bank.name.size}")
    @Schema(description = "Bank name", example = "Sberbank")
    private String bankName;

    @Pattern(regexp = BIC_PATTERN, message = "{supplier.validation.bank.bic.invalid}")
    @Schema(description = "Bank BIC (9 digits)", example = "044525225")
    private String bankBic;

    @Pattern(regexp = BANK_ACCOUNT_NUMBER_PATTERN, message = "{supplier.validation.bank.account.invalid}")
    @Schema(description = "Bank account number (20 digits)", example = "40702810123456789012")
    private String bankAccount;

    @Pattern(regexp = CORRESPONDENT_ACCOUNT_PATTERN, message = "{supplier.validation.correspondent.account.invalid}")
    @Schema(description = "Correspondent account (20 digits)", example = "30101810400000000225")
    private String correspondentAccount;

    // =========================================================================
    // BUSINESS TERMS
    // =========================================================================

    @Schema(description = "Average delivery time in days", example = "3")
    private Integer deliveryTime;

    @Schema(description = "Minimum order amount", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Payment delay in days", example = "30")
    private Integer paymentDelay;

    // =========================================================================
    // HELPER METHODS - hidden from Swagger
    // =========================================================================

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasWebsite() {
        return website != null && !website.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasContactPerson() {
        return contactPerson != null && !contactPerson.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasInn() {
        return inn != null && !inn.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasKpp() {
        return kpp != null && !kpp.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasOgrn() {
        return ogrn != null && !ogrn.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasLegalAddress() {
        return legalAddress != null && !legalAddress.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasActualAddress() {
        return actualAddress != null && !actualAddress.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBankName() {
        return bankName != null && !bankName.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBankBic() {
        return bankBic != null && !bankBic.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasBankAccount() {
        return bankAccount != null && !bankAccount.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasCorrespondentAccount() {
        return correspondentAccount != null && !correspondentAccount.trim().isEmpty();
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasDeliveryTime() {
        return deliveryTime != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasMinOrderAmount() {
        return minOrderAmount != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasPaymentDelay() {
        return paymentDelay != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedPhone() {
        return phone != null ? phone.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedWebsite() {
        return website != null ? website.trim().toLowerCase() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedInn() {
        return inn != null ? inn.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedKpp() {
        return kpp != null ? kpp.trim() : null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String getNormalizedOgrn() {
        return ogrn != null ? ogrn.trim() : null;
    }
}