package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceConverter {

    private final MessageService messageService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;

    /**
     * Конвертирует сущность счета в DTO
     */
    public InvoiceDto toDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        // Получаем номера заказов
        String purchaseOrderNumber = null;
        String salesOrderNumber = null;
        String orderTypeDescription = null;

        if (invoice.isPurchaseOrder()) {
            purchaseOrderNumber = purchaseOrderRepository.findById(invoice.getPurchaseOrderId())
                    .map(order -> order.getOrderNumber())
                    .orElse(null);
            orderTypeDescription = messageService.get("invoice.order.type.purchase");
        } else if (invoice.isSalesOrder()) {
            salesOrderNumber = salesOrderRepository.findById(invoice.getSalesOrderId())
                    .map(order -> order.getOrderNumber())
                    .orElse(null);
            orderTypeDescription = messageService.get("invoice.order.type.sales");
        }

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .purchaseOrderId(invoice.getPurchaseOrderId())
                .purchaseOrderNumber(purchaseOrderNumber)
                .salesOrderId(invoice.getSalesOrderId())
                .salesOrderNumber(salesOrderNumber)
                .orderTypeDescription(orderTypeDescription)
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .localizedStatus(invoice.getStatus().getLocalizedName(messageService))
                .paymentMethod(invoice.getPaymentMethod())
                .localizedPaymentMethod(invoice.getPaymentMethod().getLocalizedName(messageService))
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .paidDate(invoice.getPaidDate())
                .description(invoice.getDescription())
                .notes(invoice.getNotes())
                .createdBy(invoice.getCreatedBy())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    /**
     * Конвертирует список счетов в список DTO
     */
    public List<InvoiceDto> toDtoList(List<Invoice> invoices) {
        if (invoices == null) {
            return null;
        }
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность в DTO с дополнительной информацией о платежах
     */
    public InvoiceDto toDtoWithPayments(Invoice invoice,
                                        BigDecimal totalPaid,
                                        int paymentCount) {
        InvoiceDto dto = toDto(invoice);
        if (dto != null) {
            dto.setTotalPaid(totalPaid);
            dto.setRemainingAmount(invoice.getAmount().subtract(totalPaid));
            dto.setPaymentCount(paymentCount);

            // Вычисляем процент оплаты
            if (invoice.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                double percent = totalPaid.doubleValue() / invoice.getAmount().doubleValue() * 100;
                dto.setPaymentPercent(Math.min(100, percent));
            }
        }
        return dto;
    }

    /**
     * Конвертирует сущность в упрощенный DTO (без деталей заказа)
     */
    public InvoiceDto toSimpleDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .localizedStatus(invoice.getStatus().getLocalizedName(messageService))
                .paymentMethod(invoice.getPaymentMethod())
                .localizedPaymentMethod(invoice.getPaymentMethod().getLocalizedName(messageService))
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .paidDate(invoice.getPaidDate())
                .build();
    }
}