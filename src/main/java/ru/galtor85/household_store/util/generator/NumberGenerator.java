package ru.galtor85.household_store.util.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.finance.InvoiceValidator;

import java.util.UUID;

/**
 * Generator for order and invoice numbers with uniqueness validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NumberGenerator {

    private static final String PO_PREFIX = "PO-";
    private static final String WO_PREFIX = "WO-";
    private static final String SO_PREFIX = "SO-";
    private static final String INV_PREFIX = "INV-";
    private static final int UUID_LENGTH = 8;
    private static final int MAX_RETRY_ATTEMPTS = 100;

    private final InvoiceValidator invoiceValidator;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final LogMessageService logMsg;
    private final MessageService messageService;

    /**
     * Generates unique purchase order number.
     */
    public String generatePurchaseOrderNumber() {
        for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
            String orderNumber = PO_PREFIX + System.currentTimeMillis() + "-" + generateRandomPart();
            if (isPurchaseOrderNumberUnique(orderNumber)) {
                log.debug(logMsg.get("number.generator.po.unique.generated", orderNumber, i + 1));
                return orderNumber;
            }
            log.debug(logMsg.get("number.generator.po.collision", orderNumber, i + 1));
        }
        log.error(logMsg.get("number.generator.po.max.attempts.exceeded", MAX_RETRY_ATTEMPTS));
        throw new IllegalStateException(
                messageService.get("number.generator.po.max.attempts.exceeded", MAX_RETRY_ATTEMPTS)
        );
    }

    /**
     * Generates unique write-off number.
     */
    public String generateWriteOffNumber() {
        return WO_PREFIX + System.currentTimeMillis() + "-" + generateRandomPart();
    }

    /**
     * Generates unique sales order number.
     */
    public String generateSalesOrderNumber() {
        for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
            String orderNumber = SO_PREFIX + System.currentTimeMillis() + "-" + generateRandomPart();
            if (isSalesOrderNumberUnique(orderNumber)) {
                log.debug(logMsg.get("number.generator.so.unique.generated", orderNumber, i + 1));
                return orderNumber;
            }
            log.debug(logMsg.get("number.generator.so.collision", orderNumber, i + 1));
        }
        log.error(logMsg.get("number.generator.so.max.attempts.exceeded", MAX_RETRY_ATTEMPTS));
        throw new IllegalStateException(
                messageService.get("number.generator.so.max.attempts.exceeded", MAX_RETRY_ATTEMPTS)
        );
    }

    /**
     * Generates unique invoice number.
     */
    public String generateInvoiceNumber() {
        for (int i = 0; i < MAX_RETRY_ATTEMPTS; i++) {
            String invoiceNumber = INV_PREFIX + System.currentTimeMillis() + "-" + generateRandomPart();
            if (isInvoiceNumberUnique(invoiceNumber)) {
                log.debug(logMsg.get("number.generator.invoice.unique.generated", invoiceNumber, i + 1));
                return invoiceNumber;
            }
            log.debug(logMsg.get("number.generator.invoice.collision", invoiceNumber, i + 1));
        }
        log.error(logMsg.get("number.generator.invoice.max.attempts.exceeded", MAX_RETRY_ATTEMPTS));
        throw new IllegalStateException(
                messageService.get("number.generator.invoice.max.attempts.exceeded", MAX_RETRY_ATTEMPTS)
        );
    }

    private String generateRandomPart() {
        return UUID.randomUUID().toString().substring(0, UUID_LENGTH).toUpperCase();
    }

    private boolean isPurchaseOrderNumberUnique(String orderNumber) {
        return purchaseOrderRepository.findByOrderNumber(orderNumber).isEmpty();
    }

    private boolean isSalesOrderNumberUnique(String orderNumber) {
        return salesOrderRepository.findByOrderNumber(orderNumber).isEmpty();
    }

    private boolean isInvoiceNumberUnique(String invoiceNumber) {
        try {
            invoiceValidator.validateInvoiceNumberUnique(invoiceNumber);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}