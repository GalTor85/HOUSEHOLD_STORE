package ru.galtor85.household_store.validator.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.payment.PaymentProcessRequest;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * Validator for PaymentProcessRequest.
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestValidator {

    private final MessageService messageService;

    /**
     * Validates that exactly one payment target is specified.
     *
     * @param request the payment request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateExactlyOnePaymentTarget(PaymentProcessRequest request) {
        int count = 0;
        if (request.getOrderId() != null) count++;
        if (request.getInvoiceId() != null) count++;
        if (request.getPurchaseOrderId() != null) count++;
        if (request.getOriginalTransactionId() != null) count++;

        if (count != 1) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.exactly.one.target"));
        }
    }

    /**
     * Validates field combinations for different payment types.
     *
     * @param request the payment request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateFieldCombination(PaymentProcessRequest request) {
        // Manager receive cash payment
        if (isManagerReceiveCash(request)) {
            validateManagerReceiveCash(request);
            return;
        }

        // Manager supplier payment from bank
        if (isManagerSupplierBankPayment(request)) {
            validateManagerSupplierBankPayment(request);
            return;
        }

        // Manager supplier payment from cash
        if (isManagerSupplierCashPayment(request)) {
            validateManagerSupplierCashPayment(request);
            return;
        }

        // Customer order payment
        if (isCustomerOrderPayment(request)) {
            validateCustomerOrderPayment(request);
            return;
        }

        // Refund
        if (isRefund(request)) {
            validateRefund(request);
            return;
        }

        // Invoice payment
        if (request.getInvoiceId() != null) {
            validateInvoicePayment(request);
            return;
        }

        throw new IllegalArgumentException(
                messageService.get("payment.validation.unknown.type"));
    }

    /**
     * Validates common payment request fields.
     *
     * @param request the payment request
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCommon(PaymentProcessRequest request) {
        if (request.getPaymentMethodId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.payment.method.required"));
        }
        if (request.getAmount() != null && request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.amount.positive"));
        }
    }

    // =========================================================================
    // PRIVATE VALIDATION METHODS
    // =========================================================================

    private boolean isManagerReceiveCash(PaymentProcessRequest request) {
        return request.getCustomerId() != null &&
                request.getCashRegisterId() != null &&
                request.getInvoiceId() != null;
    }

    private boolean isManagerSupplierBankPayment(PaymentProcessRequest request) {
        return request.getPurchaseOrderId() != null && request.getBankAccountId() != null;
    }

    private boolean isManagerSupplierCashPayment(PaymentProcessRequest request) {
        return request.getPurchaseOrderId() != null && request.getCashRegisterId() != null;
    }

    private boolean isCustomerOrderPayment(PaymentProcessRequest request) {
        return request.getOrderId() != null &&
                request.getCustomerId() == null &&
                request.getPurchaseOrderId() == null;
    }

    private boolean isRefund(PaymentProcessRequest request) {
        return request.getOriginalTransactionId() != null;
    }

    private void validateManagerReceiveCash(PaymentProcessRequest request) {
        if (request.getOrderId() != null || request.getPurchaseOrderId() != null ||
                request.getOriginalTransactionId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.receive.cash.invalid.fields"));
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.amount.required"));
        }
    }

    private void validateManagerSupplierBankPayment(PaymentProcessRequest request) {
        if (request.getOrderId() != null || request.getInvoiceId() != null ||
                request.getCustomerId() != null || request.getCashRegisterId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.supplier.bank.invalid.fields"));
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.amount.required"));
        }
    }

    private void validateManagerSupplierCashPayment(PaymentProcessRequest request) {
        if (request.getOrderId() != null || request.getInvoiceId() != null ||
                request.getCustomerId() != null || request.getBankAccountId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.supplier.cash.invalid.fields"));
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.amount.required"));
        }
    }

    private void validateCustomerOrderPayment(PaymentProcessRequest request) {
        if (request.getInvoiceId() != null || request.getPurchaseOrderId() != null ||
                request.getOriginalTransactionId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.customer.order.invalid.fields"));
        }
    }

    private void validateRefund(PaymentProcessRequest request) {
        if (request.getOrderId() != null || request.getInvoiceId() != null ||
                request.getPurchaseOrderId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.refund.invalid.fields"));
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.amount.required"));
        }
        if (request.getRefundReason() == null || request.getRefundReason().isBlank()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.refund.reason.required"));
        }
    }

    private void validateInvoicePayment(PaymentProcessRequest request) {
        if (request.getOrderId() != null || request.getPurchaseOrderId() != null ||
                request.getOriginalTransactionId() != null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.invoice.invalid.fields"));
        }
    }
}