package ru.galtor85.household_store.dto.response.supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Supplier DTO", title = "Supplier")
public class SupplierDto {

    @Schema(description = "Supplier ID", example = "1")
    private Long id;

    @Schema(description = "Company name", example = "ООО 'ТехноПост'")
    private String name;

    @Schema(description = "Contact email", example = "info@technopost.ru")
    private String email;

    @Schema(description = "Contact phone", example = "+7 (495) 123-45-67")
    private String phone;

    @Schema(description = "Website", example = "https://www.technopost.ru")
    private String website;

    @Schema(description = "Contact person", example = "Иванов Иван Иванович")
    private String contactPerson;

    @Schema(description = "INN", example = "7701234567")
    private String inn;

    @Schema(description = "KPP", example = "770101001")
    private String kpp;

    @Schema(description = "OGRN", example = "1027701234567")
    private String ogrn;

    @Schema(description = "Legal address", example = "г. Москва, ул. Ленина, д. 1")
    private String legalAddress;

    @Schema(description = "Actual address", example = "г. Москва, ул. Ленина, д. 1")
    private String actualAddress;

    @Schema(description = "Bank name", example = "ПАО Сбербанк")
    private String bankName;

    @Schema(description = "Bank BIC", example = "044525225")
    private String bankBic;

    @Schema(description = "Bank account", example = "40702810123456789012")
    private String bankAccount;

    @Schema(description = "Correspondent account", example = "30101810400000000225")
    private String correspondentAccount;

    @Schema(description = "Supplier status", example = "ACTIVE")
    private SupplierStatus status;

    @Schema(description = "Localized status", example = "Активен")
    private String localizedStatus;

    @Schema(description = "Average rating", example = "4.5")
    private Double rating;

    @Schema(description = "Number of ratings", example = "10")
    private Integer ratingCount;

    @Schema(description = "Average delivery time in days", example = "3")
    private Integer deliveryTime;

    @Schema(description = "Minimum salesOrder amount", example = "10000.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Payment delay in days", example = "30")
    private Integer paymentDelay;

    @Schema(description = "Created by manager ID", example = "1")
    private Long createdBy;

    @Schema(description = "Creation timestamp", example = "2024-01-01T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-02T15:45:00")
    private LocalDateTime updatedAt;
}