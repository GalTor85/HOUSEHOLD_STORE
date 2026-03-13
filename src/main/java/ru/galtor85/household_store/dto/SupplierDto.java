package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.SupplierStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supplier DTO", title = "Supplier")
public class SupplierDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String website;
    private String contactPerson;
    private String inn;
    private String kpp;
    private String ogrn;
    private String legalAddress;
    private String actualAddress;
    private String bankName;
    private String bankBic;
    private String bankAccount;
    private String correspondentAccount;
    private SupplierStatus status;
    private String localizedStatus;
    private Double rating;
    private Integer ratingCount;
    private Integer deliveryTime;
    private java.math.BigDecimal minOrderAmount;
    private Integer paymentDelay;
    private LocalDateTime createdAt;
}