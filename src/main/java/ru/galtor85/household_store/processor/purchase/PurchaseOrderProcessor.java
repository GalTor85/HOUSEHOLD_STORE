package ru.galtor85.household_store.processor.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.order.PurchaseOrderBuilder;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.PurchaseOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.processor.invoice.InvoiceAutoCreationProcessor;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.order.PurchaseOrderValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderProcessor {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderBuilder builder;
    private final InvoiceAutoCreationProcessor invoiceAutoCreationProcessor;
    private final PurchaseOrderValidator validator;
    private final MessageService messageService;

    // =========================================================================
    // CREATE PURCHASE ORDER
    // =========================================================================

    /**
     * Creates a new purchase order
     *
     * @param request   purchase order creation request
     * @param supplier  supplier entity
     * @param products  list of products
     * @param prices    list of prices for each product
     * @param managerId ID of the manager creating the order
     * @return created purchase order entity
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderCreateRequest request,
                                             Supplier supplier,
                                             List<Product> products,
                                             List<BigDecimal> prices,
                                             Long managerId) {

        log.info(messageService.get("purchase.order.processor.create.start",
                request.getSupplierId(), managerId));

        // Validate request and supplier
        validator.validateCreateRequest(request);
        validator.validateSupplierActive(supplier);

        // Build order
        PurchaseOrder order = builder.buildOrder(request, managerId);

        // Build order items
        List<PurchaseOrderItem> items = builder.buildOrderItems(
                order,
                request.getItems(),
                products,
                prices
        );

        // Calculate totals
        BigDecimal totalAmount = builder.calculateTotalAmount(items);

        // Set order data
        order.setItems(items);
        order.setSubtotal(totalAmount);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);

        // Save order
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        // Create invoice automatically
        Invoice invoice = invoiceAutoCreationProcessor.createInvoiceForOrder(order, managerId);
        if (invoice != null) {
            savedOrder.addInvoice(invoice);
            purchaseOrderRepository.save(savedOrder);
            log.info(messageService.get("purchase.order.processor.invoice.created",
                    invoice.getInvoiceNumber(), savedOrder.getOrderNumber()));
        }

        log.info(messageService.get("purchase.order.processor.create.complete",
                savedOrder.getOrderNumber(), managerId, items.size(), totalAmount));

        return savedOrder;
    }

    // =========================================================================
    // CANCEL PURCHASE ORDER
    // =========================================================================

    /**
     * Cancels a purchase order
     *
     * @param order       purchase order to cancel
     * @param reason      cancellation reason
     * @param cancelledBy ID of the user cancelling the order
     * @return cancelled purchase order
     */
    @Transactional
    public PurchaseOrder cancelOrder(PurchaseOrder order, String reason, Long cancelledBy) {
        log.info(messageService.get("purchase.order.processor.cancel.start",
                order.getOrderNumber(), reason));

        // Update status
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // Cancel all pending invoices
        for (Invoice invoice : order.getInvoices()) {
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
            }
        }

        // Save
        PurchaseOrder cancelled = purchaseOrderRepository.save(order);

        log.info(messageService.get("purchase.order.processor.cancel.complete",
                order.getOrderNumber(), cancelledBy));

        return cancelled;
    }
}