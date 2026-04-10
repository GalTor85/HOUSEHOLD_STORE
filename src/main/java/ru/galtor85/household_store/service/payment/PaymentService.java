package ru.galtor85.household_store.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.finance.InsufficientFundsException;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.converter.PaymentTransactionConverter;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.entity.common.OperationStatus;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;
import ru.galtor85.household_store.repository.finance.BankAccountRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.repository.payment.PaymentTransactionRepository;
import ru.galtor85.household_store.service.cash.CashTransactionService;
import ru.galtor85.household_store.service.currency.CurrencyService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.math.BigDecimalUtils;
import ru.galtor85.household_store.validator.cash.CashRegisterValidator;
import ru.galtor85.household_store.validator.finance.BankAccountValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Unified payment service for all payment methods
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final InvoiceRepository invoiceRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CashTransactionService cashTransactionService;
    private final CashRegisterValidator cashRegisterValidator;
    private final BankAccountValidator bankAccountValidator;
    private final PaymentTransactionConverter paymentTransactionConverter;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;
    private final BigDecimalUtils bigDecimalUtils;
    private final CurrencyService currencyService;

    // =========================================================================
    // CUSTOMER PAYMENTS
    // =========================================================================

    /**
     * Customer pays for their sales order using saved payment method
     *
     * @param salesOrderId    sales order ID
     * @param paymentMethodId payment method ID (card, wallet, etc.)
     * @param userId          customer ID
     * @return payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto customerPayOrder(Long salesOrderId, Long paymentMethodId, Long userId) {
        log.info(messageService.get("payment.customer.pay.start", salesOrderId, userId));

        PaymentMethod paymentMethod = validatePaymentMethod(paymentMethodId, userId);

        Invoice invoice = findInvoiceBySalesOrder(salesOrderId);
        validateInvoicePayable(invoice);

        BigDecimal remainingAmount = invoice.getRemainingAmount();

        PaymentTransaction transaction = createPaymentTransaction(
                paymentMethod, invoice, remainingAmount,
                messageService.get("payment.customer.order.description", salesOrderId)
        );

        try {
            PaymentGateway gateway = gatewayFactory.getGateway(paymentMethod.getProvider());
            PaymentResult result = gateway.processPayment(
                    paymentMethod, remainingAmount, invoice.getCurrency(),
                    messageService.get("payment.customer.order.description", salesOrderId)
            );

            if (result.isSuccess()) {
                completeTransaction(transaction, result);
                updateInvoiceAfterPayment(invoice);
                log.info(messageService.get("payment.customer.pay.success", salesOrderId, transaction.getId()));
            } else {
                failTransaction(transaction, result.getErrorMessage());
                log.error(messageService.get("payment.customer.pay.failed", salesOrderId, result.getErrorMessage()));
            }

            return paymentTransactionConverter.toDto(paymentTransactionRepository.save(transaction));

        } catch (Exception e) {
            failTransaction(transaction, e.getMessage());
            paymentTransactionRepository.save(transaction);
            log.error(messageService.get("payment.customer.pay.error", salesOrderId, e.getMessage()), e);
            throw new RuntimeException(messageService.get("payment.process.error", e.getMessage()), e);
        }
    }

    // =========================================================================
    // MANAGER PAYMENTS - SUPPLIER
    // =========================================================================

    /**
     * Manager pays supplier for purchase order from bank account
     *
     * @param bankAccountId   company bank account ID
     * @param purchaseOrderId purchase order ID
     * @param amount          payment amount
     * @return payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerPaySupplierFromBank(Long bankAccountId, Long purchaseOrderId,
                                                            BigDecimal amount) {
        log.info(messageService.get("payment.manager.supplier.bank.start", purchaseOrderId, bankAccountId));

        BankAccount bankAccount = bankAccountValidator.validateExists(bankAccountId);
        bankAccountValidator.validateActive(bankAccount);

        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(bankAccountId, bankAccount.getBalance(), amount);
        }

        Invoice invoice = findInvoiceByPurchaseOrder(purchaseOrderId);
        validateInvoicePayable(invoice);

        // Withdraw from bank account
        bankAccount.withdraw(amount);
        bankAccountRepository.save(bankAccount);

        // Create payment transaction
        PaymentTransaction transaction = createSupplierPaymentTransaction(
                invoice, amount, BANK_ACCOUNT_TYPE, bankAccountId.toString()
        );
        completeTransaction(transaction, PaymentResult.success(BANK_TXN_PREFIX + System.currentTimeMillis()));

        updateInvoiceAfterPayment(invoice);

        log.info(messageService.get("payment.manager.supplier.bank.success", purchaseOrderId, amount));

        return paymentTransactionConverter.toDto(paymentTransactionRepository.save(transaction));
    }

    /**
     * Manager pays supplier for purchase order from cash register
     *
     * @param cashRegisterId cash register ID
     * @param invoiceId      invoice ID
     * @param amount         payment amount
     * @param managerId      manager ID
     * @return payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerPaySupplierFromCash(Long cashRegisterId, Long invoiceId,
                                                            BigDecimal amount, Long managerId) {
        log.info(messageService.get("payment.manager.supplier.cash.start", invoiceId, cashRegisterId));

        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        cashRegisterValidator.validateActive(cashRegister);
        cashRegisterValidator.validateSufficientFunds(cashRegister, amount);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("invoice.not.found", invoiceId)));

        validateInvoicePayable(invoice);

        // Create expense transaction in cash register
        CashTransactionRequest expenseRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.EXPENSE)
                .amount(amount)
                .invoiceId(invoiceId)
                .purchaseOrderId(invoice.getPurchaseOrderId())
                .description(messageService.get("payment.supplier.payment.description",
                        invoice.getPurchaseOrderId()))
                .build();

        cashTransactionService.createTransaction(expenseRequest, managerId);

        // Create payment transaction
        PaymentTransaction transaction = createSupplierPaymentTransaction(
                invoice, amount, CASH_REGISTER_TYPE, cashRegisterId.toString()
        );
        completeTransaction(transaction, PaymentResult.success(CASH_REGISTER_TXN_PREFIX + System.currentTimeMillis()));

        updateInvoiceAfterPayment(invoice);

        log.info(messageService.get("payment.manager.supplier.cash.success", invoiceId, amount));

        return paymentTransactionConverter.toDto(paymentTransactionRepository.save(transaction));
    }

    // =========================================================================
    // MANAGER PAYMENTS - CUSTOMER CASH
    // =========================================================================

    /**
     * Manager receives cash payment from customer at point of sale
     *
     * @param cashRegisterId cash register ID
     * @param invoiceId      invoice ID
     * @param amount         payment amount
     * @param customerId     customer ID
     * @param managerId      manager ID
     * @return payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerReceiveCashPayment(Long cashRegisterId, Long invoiceId,
                                                           BigDecimal amount, Long customerId,
                                                           Long managerId) {
        log.info(messageService.get("payment.manager.receive.cash.start", invoiceId, amount));

        if (customerId == null) {
            throw new IllegalArgumentException(messageService.get("payment.customer.id.required"));
        }

        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        cashRegisterValidator.validateActive(cashRegister);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("invoice.not.found", invoiceId)));

        validateInvoicePayable(invoice);

        // Create income transaction in cash register
        CashTransactionRequest incomeRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.INCOME)
                .amount(amount)
                .invoiceId(invoiceId)
                .customerId(customerId)
                .salesOrderId(invoice.getSalesOrderId())
                .description(messageService.get("payment.customer.cash.payment.description",
                        invoice.getSalesOrderId(), customerId))
                .build();

        cashTransactionService.createTransaction(incomeRequest, managerId);

        // Create payment transaction
        PaymentTransaction transaction = createCustomerPaymentTransaction(
                invoice, amount, cashRegisterId.toString(), customerId
        );
        completeTransaction(transaction, PaymentResult.success(CASH_REGISTER_TXN_PREFIX + System.currentTimeMillis()));

        updateInvoiceAfterPayment(invoice);

        log.info(messageService.get("payment.manager.receive.cash.success", invoiceId, amount));

        return paymentTransactionConverter.toDto(paymentTransactionRepository.save(transaction));
    }

    // =========================================================================
    // MANAGER REFUNDS
    // =========================================================================

    /**
     * Manager processes cash refund to customer
     *
     * @param cashRegisterId        cash register ID
     * @param originalTransactionId original payment transaction ID
     * @param amount                refund amount
     * @param reason                refund reason
     * @param managerId             manager ID
     * @return refund transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerProcessCashRefund(Long cashRegisterId, Long originalTransactionId,
                                                          BigDecimal amount, String reason, Long managerId) {
        log.info(messageService.get("payment.manager.refund.cash.start", originalTransactionId, amount));

        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        cashRegisterValidator.validateActive(cashRegister);
        cashRegisterValidator.validateSufficientFunds(cashRegister, amount);

        PaymentTransaction originalTransaction = paymentTransactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.transaction.not.found", originalTransactionId)));

        if (originalTransaction.getStatus() != PaymentTransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    messageService.get("payment.transaction.not.completed", originalTransactionId));
        }

        boolean alreadyRefunded = paymentTransactionRepository.existsByOriginalTransactionId(originalTransactionId);
        if (alreadyRefunded) {
            throw new IllegalStateException(
                    messageService.get("payment.transaction.already.refunded", originalTransactionId));
        }

        Invoice invoice = invoiceRepository.findById(originalTransaction.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("invoice.not.found", originalTransaction.getInvoiceId())));

        // Create refund transaction in cash register (EXPENSE for refund)
        CashTransactionRequest refundRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.EXPENSE)
                .amount(amount)
                .originalTransactionId(originalTransactionId)
                .invoiceId(invoice.getId())
                .description(messageService.get("payment.cash.refund.description",
                        originalTransactionId, reason))
                .build();

        cashTransactionService.createTransaction(refundRequest, managerId);

        // Create refund payment transaction
        PaymentTransaction refundTransaction = createRefundTransaction(originalTransaction, amount, reason);

        // Mark original as refunded
        originalTransaction.setStatus(PaymentTransactionStatus.REFUNDED);
        paymentTransactionRepository.save(originalTransaction);

        log.info(messageService.get("payment.manager.refund.cash.success", originalTransactionId));

        return paymentTransactionConverter.toDto(paymentTransactionRepository.save(refundTransaction));
    }

    // =========================================================================
    // QUERY METHODS
    // =========================================================================

    /**
     * Get payment transaction by ID
     *
     * @param transactionId transaction ID
     * @return payment transaction DTO
     */
    @Transactional(readOnly = true)
    public PaymentTransactionDto getPaymentTransaction(Long transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.transaction.not.found", transactionId)));
        return paymentTransactionConverter.toDto(transaction);
    }

    /**
     * Get supplier payment history for purchase order
     *
     * @param purchaseOrderId purchase order ID
     * @return list of payment transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getSupplierPayments(Long purchaseOrderId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByOrderIdAndOrderType(purchaseOrderId, OrderType.PURCHASE);
        return paymentTransactionConverter.toDtoList(transactions);
    }

    /**
     * Get customer payment history for sales order
     *
     * @param salesOrderId sales order ID
     * @return list of payment transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getCustomerPayments(Long salesOrderId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByOrderIdAndOrderType(salesOrderId, OrderType.SALES);
        return paymentTransactionConverter.toDtoList(transactions);
    }

    /**
     * Get cash register balance
     *
     * @param cashRegisterId cash register ID
     * @return current balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getCashRegisterBalance(Long cashRegisterId) {
        CashRegister cashRegister = cashRegisterValidator.validateExists(cashRegisterId);
        return cashRegister.getCurrentBalance();
    }

    /**
     * Get bank account balance
     *
     * @param bankAccountId bank account ID
     * @return current balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getBankAccountBalance(Long bankAccountId) {
        BankAccount bankAccount = bankAccountValidator.validateExists(bankAccountId);
        return bankAccount.getBalance();
    }

    // =========================================================================
    // LEGACY METHODS (for backward compatibility)
    // =========================================================================

    /**
     * Process payment using specified payment method (legacy)
     *
     * @param paymentMethodId payment method ID
     * @param amount          payment amount
     * @param currency        payment currency
     * @param invoiceId       associated invoice ID
     * @param orderId         associated order ID
     * @param orderType       order type (PURCHASE, SALES)
     * @param description     payment description
     * @return payment transaction
     */
    @Transactional
    public PaymentTransaction processPayment(Long paymentMethodId,
                                             BigDecimal amount,
                                             String currency,
                                             Long invoiceId,
                                             Long orderId,
                                             OrderType orderType,
                                             String description
    ) {
        log.info(messageService.get("payment.service.process.start",
                paymentMethodId, amount, currency));

        if (currency == null || currency.isBlank()) {
            currency = financialConfig.getDefaultCurrency();
        }

        currencyService.getCurrencyByCode(currency);

        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.method.not.found", paymentMethodId)));

        if (!paymentMethod.isActive()) {
            throw new IllegalStateException(
                    messageService.get("payment.method.inactive", paymentMethodId));
        }

        if (!paymentMethod.validate()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.method.invalid", paymentMethodId));
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .paymentMethodId(paymentMethodId)
                .invoiceId(invoiceId)
                .orderId(orderId)
                .orderType(orderType)
                .amount(amount)
                .currency(currency)
                .status(PaymentTransactionStatus.PENDING)
                .description(description)
                .processingFee(paymentMethod.getProcessingFee())
                .netAmount(calculateNetAmount(amount, paymentMethod.getProcessingFee()))
                .createdAt(LocalDateTime.now())
                .build();

        transaction = paymentTransactionRepository.save(transaction);

        PaymentGateway gateway = gatewayFactory.getGateway(paymentMethod.getProvider());

        try {
            PaymentResult result = gateway.processPayment(paymentMethod, amount, currency, description);

            if (result.isSuccess()) {
                transaction.setStatus(PaymentTransactionStatus.COMPLETED);
                transaction.setProviderTransactionId(result.getTransactionId());
                transaction.setCompletedAt(LocalDateTime.now());
                log.info(messageService.get("payment.service.success",
                        transaction.getId(), result.getTransactionId()));
            } else {
                transaction.setStatus(PaymentTransactionStatus.FAILED);
                transaction.setErrorMessage(result.getErrorMessage());
                log.error(messageService.get("payment.service.failed",
                        transaction.getId(), result.getErrorMessage()));
            }

            return paymentTransactionRepository.save(transaction);

        } catch (Exception e) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            paymentTransactionRepository.save(transaction);
            throw new RuntimeException(
                    messageService.get("payment.service.error", e.getMessage()), e);
        }
    }

    /**
     * Refund payment (legacy)
     *
     * @param transactionId original payment transaction ID
     * @param reason        refund reason
     * @return refund transaction
     */
    @Transactional
    public PaymentTransaction refundPayment(Long transactionId, String reason) {
        log.info(messageService.get("payment.service.refund.start", transactionId, reason));

        PaymentTransaction original = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.transaction.not.found", transactionId)));

        if (original.getStatus() != PaymentTransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    messageService.get("payment.transaction.not.completed", transactionId));
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(original.getPaymentMethodId())
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.method.not.found", original.getPaymentMethodId())));

        PaymentGateway gateway = gatewayFactory.getGateway(paymentMethod.getProvider());

        PaymentResult result = gateway.refundPayment(paymentMethod, original.getProviderTransactionId(),
                original.getAmount(), reason);

        PaymentTransaction refund = PaymentTransaction.builder()
                .paymentMethodId(original.getPaymentMethodId())
                .invoiceId(original.getInvoiceId())
                .orderId(original.getOrderId())
                .orderType(original.getOrderType())
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .status(result.isSuccess() ? PaymentTransactionStatus.REFUNDED : PaymentTransactionStatus.FAILED)
                .description(TransactionType.REFUND + ": " + reason)
                .providerTransactionId(result.getTransactionId())
                .completedAt(result.isSuccess() ? LocalDateTime.now() : null)
                .errorMessage(result.getErrorMessage())
                .build();

        if (result.isSuccess()) {
            original.setStatus(PaymentTransactionStatus.REFUNDED);
            paymentTransactionRepository.save(original);
        }

        log.info(messageService.get("payment.service.refund.complete", transactionId,
                result.isSuccess() ? OperationStatus.SUCCESS.getCode() : OperationStatus.FAILED.getCode()));

        return paymentTransactionRepository.save(refund);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    private PaymentMethod validatePaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.method.not.found", paymentMethodId)));

        if (!paymentMethod.isActive()) {
            throw new IllegalStateException(
                    messageService.get("payment.method.inactive", paymentMethodId));
        }

        if (!paymentMethod.getCreatedBy().equals(userId)) {
            throw new SecurityException(
                    messageService.get("payment.method.not.belong.to.user", paymentMethodId, userId));
        }

        if (!paymentMethod.validate()) {
            throw new IllegalArgumentException(
                    messageService.get("payment.method.invalid", paymentMethodId));
        }

        return paymentMethod;
    }

    private Invoice findInvoiceBySalesOrder(Long salesOrderId) {
        return invoiceRepository.findBySalesOrderId(salesOrderId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING ||
                        i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("invoice.not.found.for.order", salesOrderId)));
    }

    private Invoice findInvoiceByPurchaseOrder(Long purchaseOrderId) {
        return invoiceRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING ||
                        i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("invoice.not.found.for.purchase.order", purchaseOrderId)));
    }

    private void validateInvoicePayable(Invoice invoice) {
        if (!invoice.isPayable()) {
            throw new IllegalStateException(
                    messageService.get("invoice.not.payable", invoice.getId(), invoice.getStatus()));
        }
    }

    private PaymentTransaction createPaymentTransaction(PaymentMethod paymentMethod, Invoice invoice,
                                                        BigDecimal amount, String description) {
        return PaymentTransaction.builder()
                .paymentMethodId(paymentMethod.getId())
                .invoiceId(invoice.getId())
                .orderId(invoice.getOrderId())
                .orderType(invoice.isPurchaseOrder() ? OrderType.PURCHASE : OrderType.SALES)
                .amount(amount)
                .currency(invoice.getCurrency())
                .status(PaymentTransactionStatus.PENDING)
                .description(description)
                .processingFee(paymentMethod.getProcessingFee())
                .netAmount(calculateNetAmount(amount, paymentMethod.getProcessingFee()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentTransaction createSupplierPaymentTransaction(Invoice invoice, BigDecimal amount,
                                                                String sourceType, String sourceId
    ) {
        return PaymentTransaction.builder()
                .paymentMethodId(null)
                .invoiceId(invoice.getId())
                .orderId(invoice.getPurchaseOrderId())
                .orderType(OrderType.PURCHASE)
                .amount(amount)
                .currency(invoice.getCurrency())
                .status(PaymentTransactionStatus.PENDING)
                .description(messageService.get("payment.supplier.payment.from", sourceType, sourceId))
                .providerTransactionId(sourceType + "_" + sourceId + "_" + System.currentTimeMillis())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentTransaction createCustomerPaymentTransaction(Invoice invoice, BigDecimal amount,
                                                                String paymentId,
                                                                Long customerId) {
        return PaymentTransaction.builder()
                .paymentMethodId(null)
                .invoiceId(invoice.getId())
                .orderId(invoice.getSalesOrderId())
                .orderType(OrderType.SALES)
                .amount(amount)
                .currency(invoice.getCurrency())
                .status(PaymentTransactionStatus.PENDING)
                .description(messageService.get("payment.customer.payment.received", CASH_PAYMENT_TYPE, customerId))
                .providerTransactionId(CASH_TXN_PREFIX + "_" + paymentId + "_" + System.currentTimeMillis())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentTransaction createRefundTransaction(PaymentTransaction original,
                                                       BigDecimal amount,
                                                       String reason) {
        return PaymentTransaction.builder()
                .paymentMethodId(original.getPaymentMethodId())
                .invoiceId(original.getInvoiceId())
                .orderId(original.getOrderId())
                .orderType(original.getOrderType())
                .amount(amount)
                .currency(original.getCurrency())
                .status(PaymentTransactionStatus.REFUNDED)
                .description(messageService.get("payment.refund.description", reason))
                .providerTransactionId(REFUND_TXN_PREFIX + System.currentTimeMillis())
                .originalTransactionId(original.getId())
                .completedAt(LocalDateTime.now())
                .build();
    }

    private void completeTransaction(PaymentTransaction transaction, PaymentResult result) {
        transaction.setStatus(PaymentTransactionStatus.COMPLETED);
        transaction.setProviderTransactionId(result.getTransactionId());
        transaction.setProviderPaymentUrl(result.getPaymentUrl());
        transaction.setCompletedAt(LocalDateTime.now());
        if (result.hasFeeInfo()) {
            transaction.setProcessingFee(result.getFee());
            transaction.setNetAmount(result.getNetAmount());
        }
    }

    private void failTransaction(PaymentTransaction transaction, String errorMessage) {
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setErrorMessage(errorMessage);
    }

    private void updateInvoiceAfterPayment(Invoice invoice) {
        BigDecimal totalPaid = invoice.getTotalPaidAmount();

        if (totalPaid.compareTo(invoice.getAmount()) >= 0) {
            invoice.markAsPaid();
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);
    }

    private BigDecimal calculateNetAmount(BigDecimal amount, BigDecimal feePercent) {
        // Use BigDecimalUtils for null and zero check
        if (bigDecimalUtils.isNullOrZero(feePercent)) {
            return amount;
        }
        // Use BigDecimalUtils for percentage calculation
        return bigDecimalUtils.applyPercentageDiscount(amount, feePercent);
    }
}