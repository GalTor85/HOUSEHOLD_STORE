package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceConverter {

    private final MessageService messageService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;

    /**
     * Converts an invoice entity to DTO with payment information
     *
     * @param invoice   the invoice entity
     * @param totalPaid total paid amount (calculated by service)
     * @return InvoiceDto with all fields
     */
    public InvoiceDto toDto(Invoice invoice, BigDecimal totalPaid) {
        if (invoice == null) {
            return null;
        }

        BigDecimal remainingAmount = invoice.getAmount().subtract(totalPaid);

        // Calculate payment percentage
        Double paymentPercent = null;
        if (invoice.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            paymentPercent = totalPaid.doubleValue() / invoice.getAmount().doubleValue() * 100;
            paymentPercent = Math.min(100.0, Math.max(0.0, paymentPercent));
        }

        // Get order numbers
        String purchaseOrderNumber = null;
        String salesOrderNumber = null;
        String orderTypeDescription = null;

        if (invoice.isPurchaseOrder()) {
            purchaseOrderNumber = purchaseOrderRepository.findById(invoice.getPurchaseOrderId())
                    .map(PurchaseOrder::getOrderNumber)
                    .orElse(null);
            orderTypeDescription = messageService.get("invoice.order.type.purchase");
        } else if (invoice.isSalesOrder()) {
            salesOrderNumber = salesOrderRepository.findById(invoice.getSalesOrderId())
                    .map(SalesOrder::getOrderNumber)
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
                .totalPaid(totalPaid)
                .remainingAmount(remainingAmount)
                .paymentPercent(paymentPercent)
                .build();
    }
}