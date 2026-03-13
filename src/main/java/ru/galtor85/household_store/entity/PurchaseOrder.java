package ru.galtor85.household_store.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders", schema = "household_schema")
public class PurchaseOrder {

    @Id
    private Long orderId; // Связь с основным заказом 1-к-1

    @OneToOne
    @MapsId
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "actual_delivery")
    private LocalDate actualDelivery;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Column(name = "received_by")
    private Long receivedBy; // Кто принял товар

    @Column(name = "quality_check")
    private Boolean qualityCheck; // Прошел ли проверку качества

    @Column(name = "invoice_number")
    private String invoiceNumber; // Номер счета-фактуры

    @Column(name = "payment_due")
    private LocalDate paymentDue; // Срок оплаты поставщику

    @Column(name = "payment_status")
    private String paymentStatus; // Статус оплаты поставщику
}