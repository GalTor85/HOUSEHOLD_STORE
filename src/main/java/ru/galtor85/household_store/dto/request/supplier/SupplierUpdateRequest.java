package ru.galtor85.household_store.dto.request.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for updating a supplier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier update request DTO", title = "Supplier Update Request")
public class SupplierUpdateRequest {

    // =========================================================================
    // BASIC INFORMATION
    // =========================================================================

    @Size(min = MIN_SUPPLIER_NAME_LENGTH, max = MAX_SUPPLIER_NAME_LENGTH, message = "{supplier.validation.name.size}")
    @Schema(description = "Company name", example = "TechnoPost LLC")
    private String name;

    @Email(message = "{supplier.validation.email.invalid}")
    @Size(max = MAX_EMAIL_LENGTH, message = "{supplier.validation.email.size}")
    @Schema(description = "Contact email", example = "info@technopost.ru")
    private String email;

    @Pattern(regexp = PHONE_PATTERN, message = "{supplier.validation.phone.invalid}")
    @Schema(description = "Contact phone", example = "+7 (495) 123-45-67")
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
    // STATUS AND BUSINESS TERMS
    // =========================================================================

    @Schema(description = "Supplier status", example = "ACTIVE",
            allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "BLOCKED", "VERIFIED"})
    private String status;

    @Schema(description = "Average delivery time in days", example = "3")
    private Integer deliveryTime;

    @Schema(description = "Minimum order amount", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Payment delay in days", example = "30")
    private Integer paymentDelay;
}