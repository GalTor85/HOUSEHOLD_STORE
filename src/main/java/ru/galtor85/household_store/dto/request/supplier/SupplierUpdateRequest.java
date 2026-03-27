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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier update request DTO", title = "Supplier Update Request")
public class SupplierUpdateRequest {

    @Size(min = 2, max = 200, message = "{supplier.validation.name.size}")
    @Schema(description = "Company name", example = "ООО 'ТехноПост'")
    private String name;

    @Email(message = "{supplier.validation.email.invalid}")
    @Size(max = 100, message = "{supplier.validation.email.size}")
    @Schema(description = "Contact email", example = "info@technopost.ru")
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,15}$", message = "{supplier.validation.phone.invalid}")
    @Schema(description = "Contact phone", example = "+7 (495) 123-45-67")
    private String phone;

    @Schema(description = "Website", example = "https://www.technopost.ru")
    private String website;

    @Size(max = 100, message = "{supplier.validation.contact.person.size}")
    @Schema(description = "Contact person", example = "Иванов Иван Иванович")
    private String contactPerson;

    @Pattern(regexp = "^\\d{10}$|^\\d{12}$", message = "{supplier.validation.inn.invalid}")
    @Schema(description = "INN (10 or 12 digits for Russian companies)", example = "7701234567")
    private String inn;

    @Pattern(regexp = "^\\d{9}$", message = "{supplier.validation.kpp.invalid}")
    @Schema(description = "KPP (9 digits for Russian companies)", example = "770101001")
    private String kpp;

    @Pattern(regexp = "^\\d{13}$", message = "{supplier.validation.ogrn.invalid}")
    @Schema(description = "OGRN (13 digits for Russian companies)", example = "1027701234567")
    private String ogrn;

    @Size(max = 500, message = "{supplier.validation.legal.address.size}")
    @Schema(description = "Legal address", example = "г. Москва, ул. Ленина, д. 1")
    private String legalAddress;

    @Size(max = 500, message = "{supplier.validation.actual.address.size}")
    @Schema(description = "Actual address", example = "г. Москва, ул. Ленина, д. 1")
    private String actualAddress;

    @Size(max = 200, message = "{supplier.validation.bank.name.size}")
    @Schema(description = "Bank name", example = "ПАО Сбербанк")
    private String bankName;

    @Pattern(regexp = "^\\d{9}$", message = "{supplier.validation.bank.bic.invalid}")
    @Schema(description = "Bank BIC (9 digits)", example = "044525225")
    private String bankBic;

    @Pattern(regexp = "^\\d{20}$", message = "{supplier.validation.bank.account.invalid}")
    @Schema(description = "Bank account number (20 digits)", example = "40702810123456789012")
    private String bankAccount;

    @Pattern(regexp = "^\\d{20}$", message = "{supplier.validation.correspondent.account.invalid}")
    @Schema(description = "Correspondent account (20 digits)", example = "30101810400000000225")
    private String correspondentAccount;

    @Schema(description = "Supplier status", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE", "INACTIVE", "BLOCKED", "VERIFIED"})
    private String status;

    @Schema(description = "Average delivery time in days", example = "3")
    private Integer deliveryTime;

    @Schema(description = "Minimum salesOrder amount", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Payment delay in days", example = "30")
    private Integer paymentDelay;
}