package ru.galtor85.household_store.entity.supplier;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "suppliers", schema = "household_schema",
        indexes = {
                @Index(name = "idx_suppliers_email", columnList = "email"),
                @Index(name = "idx_suppliers_status", columnList = "status")
        })
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    private String website;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "inn", unique = true)
    private String inn;

    @Column(name = "kpp")
    private String kpp;

    @Column(name = "ogrn")
    private String ogrn;

    @Column(name = "legal_address")
    private String legalAddress;

    @Column(name = "actual_address")
    private String actualAddress;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_bic")
    private String bankBic;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "correspondent_account")
    private String correspondentAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupplierStatus status = SupplierStatus.PENDING;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "delivery_time")
    private Integer deliveryTime;

    @Column(name = "min_order_amount")
    private java.math.BigDecimal minOrderAmount;

    @Column(name = "payment_delay")
    private Integer paymentDelay;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplierId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupplierProduct> products = new ArrayList<>();
}