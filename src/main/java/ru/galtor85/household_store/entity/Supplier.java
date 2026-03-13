package ru.galtor85.household_store.entity;

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
    private String name; // Название компании

    @Column(unique = true)
    private String email; // Контактный email

    @Column(name = "phone")
    private String phone; // Телефон

    @Column(name = "website")
    private String website; // Сайт

    @Column(name = "contact_person")
    private String contactPerson; // Контактное лицо

    @Column(name = "inn", unique = true)
    private String inn; // ИНН (для России)

    @Column(name = "kpp")
    private String kpp; // КПП (для России)

    @Column(name = "ogrn")
    private String ogrn; // ОГРН

    @Column(name = "legal_address")
    private String legalAddress; // Юридический адрес

    @Column(name = "actual_address")
    private String actualAddress; // Фактический адрес

    @Column(name = "bank_name")
    private String bankName; // Название банка

    @Column(name = "bank_bic")
    private String bankBic; // БИК

    @Column(name = "bank_account")
    private String bankAccount; // Расчетный счет

    @Column(name = "correspondent_account")
    private String correspondentAccount; // Корреспондентский счет

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupplierStatus status = SupplierStatus.PENDING; // ACTIVE, INACTIVE, PENDING, BLOCKED

    @Column(name = "rating")
    private Double rating; // Средний рейтинг (0-5)

    @Column(name = "rating_count")
    private Integer ratingCount; // Количество оценок

    @Column(name = "delivery_time")
    private Integer deliveryTime; // Среднее время доставки (в днях)

    @Column(name = "min_order_amount")
    private java.math.BigDecimal minOrderAmount; // Минимальная сумма заказа

    @Column(name = "payment_delay")
    private Integer paymentDelay; // Отсрочка платежа (в днях)

    @Column(name = "created_by")
    private Long createdBy; // ID пользователя, создавшего запись

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplierId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupplierProduct> products = new ArrayList<>(); // Поставляемые товары
}