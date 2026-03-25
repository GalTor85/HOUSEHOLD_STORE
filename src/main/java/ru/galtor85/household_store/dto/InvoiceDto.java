package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.InvoiceStatus;
import ru.galtor85.household_store.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice DTO")
public class InvoiceDto {

    private Long id;
    private String invoiceNumber;

    // Поля для заказа на закупку
    private Long purchaseOrderId;
    private String purchaseOrderNumber;

    // Поля для заказа на продажу
    private Long salesOrderId;
    private String salesOrderNumber;

    // Тип заказа (локализованный)
    private String orderTypeDescription;

    // Финансовые поля
    private BigDecimal amount;
    private String currency;
    private BigDecimal totalPaid;        // сумма оплаченных платежей
    private BigDecimal remainingAmount;  // остаток к оплате
    private Integer paymentCount;        // количество платежей
    private Double paymentPercent;       // процент оплаты

    // Статус
    private InvoiceStatus status;
    private String localizedStatus;

    // Способ оплаты
    private PaymentMethod paymentMethod;
    private String localizedPaymentMethod;

    // Даты
    private LocalDateTime issueDate;
    private LocalDateTime dueDate;
    private LocalDateTime paidDate;

    // Дополнительная информация
    private String description;
    private String notes;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Вспомогательные методы
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) &&
                status == InvoiceStatus.PENDING;
    }

    public boolean isFullyPaid() {
        return remainingAmount != null && remainingAmount.compareTo(BigDecimal.ZERO) <= 0;
    }
}