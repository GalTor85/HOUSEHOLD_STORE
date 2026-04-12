package ru.galtor85.household_store.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.finance.InsufficientFundsException;
import ru.galtor85.household_store.converter.PaymentTransactionConverter;
import ru.galtor85.household_store.dto.request.finance.BankAccountTransactionRequest;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.request.payment.PaymentProcessRequest;
import ru.galtor85.household_store.dto.response.finance.BankAccountDto;
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.dto.response.user.UserTypeAssignmentDto;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.repository.payment.PaymentMethodRepository;
import ru.galtor85.household_store.repository.payment.PaymentMethodUserTypeRepository;
import ru.galtor85.household_store.repository.payment.PaymentTransactionRepository;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.cash.CashTransactionService;
import ru.galtor85.household_store.service.finance.BankAccountService;
import ru.galtor85.household_store.service.finance.InvoiceService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Service for processing all types of payments in the system.
 *
 * <p>This service provides a unified entry point for all payment operations:
 * customer payments for sales orders, invoice payments, manager payments to suppliers
 * from bank accounts or cash registers, cash receipt from customers, and refunds.</p>
 *
 * <p>The service uses a dispatcher pattern where {@link #processPayment(PaymentProcessRequest, Long)}
 * routes requests to specific handlers based on request fields.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionConverter paymentTransactionConverter;
    private final CashTransactionService cashTransactionService;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;
    private final PaymentGatewayFactory gatewayFactory;
    private final UserTypeAssignmentService userTypeAssignmentService;
    private final PaymentMethodUserTypeRepository paymentMethodUserTypeRepository;
    private final BankAccountService bankAccountService;
    private final CashRegisterService cashRegisterService;
    private final InvoiceService invoiceService;

    // =========================================================================
    // PUBLIC DISPATCHER METHOD
    // =========================================================================

    /**
     * Unified entry point for all payment operations.
     * Routes to specific handler based on request fields.
     *
     * @param request the payment request containing all necessary data
     * @param userId the ID of the user performing the payment
     * @return payment transaction DTO with status and details
     * @throws IllegalArgumentException if payment type is unsupported
     */
    @Transactional
    public PaymentTransactionDto processPayment(PaymentProcessRequest request, Long userId) {
        log.info(messageService.get("payment.process.start", request.getPaymentTargetDescription()));

        validateCommonPaymentRequest(request);

        if (request.isCustomerOrderPayment()) {
            return customerPayOrder(request, userId);
        } else if (request.isInvoicePayment()) {
            return payInvoice(request, userId);
        } else if (request.isManagerSupplierBankPayment()) {
            return managerPaySupplierFromBank(request, userId);
        } else if (request.isManagerSupplierCashPayment()) {
            return managerPaySupplierFromCash(request, userId);
        } else if (request.isManagerReceiveCash()) {
            return managerReceiveCashPayment(request, userId);
        } else if (request.isRefund()) {
            return managerProcessCashRefund(request, userId);
        }

        throw new IllegalArgumentException(messageService.get("payment.unsupported.type"));
    }

    // =========================================================================
    // CUSTOMER PAYMENTS
    // =========================================================================

    /**
     * Customer pays for their sales order.
     *
     * @param request the payment request with order ID and payment method
     * @param userId the ID of the customer
     * @return completed payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto customerPayOrder(PaymentProcessRequest request, Long userId) {
        Long salesOrderId = request.getOrderId();
        Long paymentMethodId = request.getPaymentMethodId();

        log.info(messageService.get("payment.customer.pay.start", salesOrderId, userId));

        PaymentMethod paymentMethod = validatePaymentMethod(paymentMethodId, userId);

        List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(salesOrderId);
        InvoiceDto invoiceDto = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING || i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.no.payable.invoice.sales", salesOrderId)));

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : invoiceDto.getRemainingAmount();

        PaymentTransaction transaction = createPaymentTransaction(paymentMethod, invoiceDto, amount, userId);
        transaction = paymentTransactionRepository.save(transaction);

        try {
            PaymentGateway gateway = gatewayFactory.getGateway(paymentMethod.getProvider());
            PaymentResult result = gateway.processPayment(
                    paymentMethod, amount, invoiceDto.getCurrency(),
                    messageService.get("payment.customer.order.description", salesOrderId)
            );

            if (result.isSuccess()) {
                completeTransaction(transaction, result);
                if (amount.compareTo(invoiceDto.getRemainingAmount()) >= 0) {
                    invoiceService.markAsPaid(invoiceDto.getId(), null, userId);
                } else {
                    invoiceService.markAsPartiallyPaid(invoiceDto.getId(), amount, null, userId);
                }
                log.info(messageService.get("payment.customer.pay.success", salesOrderId, transaction.getId()));
            } else {
                failTransaction(transaction, result.getErrorMessage());
                throw new RuntimeException(result.getErrorMessage());
            }
        } catch (Exception e) {
            failTransaction(transaction, e.getMessage());
            throw new RuntimeException(messageService.get("payment.process.error", e.getMessage()), e);
        }

        return paymentTransactionConverter.toDto(transaction);
    }

    /**
     * Customer pays an invoice directly.
     *
     * @param request the payment request with invoice ID
     * @param userId the ID of the user
     * @return payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto payInvoice(PaymentProcessRequest request, Long userId) {
        Long invoiceId = request.getInvoiceId();
        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);

        if (invoice.getSalesOrderId() != null) {
            PaymentProcessRequest adaptedRequest = PaymentProcessRequest.builder()
                    .paymentMethodId(request.getPaymentMethodId())
                    .orderId(invoice.getSalesOrderId())
                    .amount(request.getAmount())
                    .build();
            return customerPayOrder(adaptedRequest, userId);
        } else {
            if (!isManager(userId)) {
                throw new SecurityException(messageService.get("payment.manager.only"));
            }
            return managerPaySupplierFromBank(request, userId);
        }
    }

    // =========================================================================
    // MANAGER PAYMENTS
    // =========================================================================

    /**
     * Manager pays supplier from company bank account.
     *
     * @param request the payment request with purchase order ID and bank account ID
     * @param managerId the ID of the manager
     * @return completed payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerPaySupplierFromBank(PaymentProcessRequest request, Long managerId) {
        Long purchaseOrderId = request.getPurchaseOrderId();
        Long bankAccountId = request.getBankAccountId();
        BigDecimal amount = request.getAmount();

        log.info(messageService.get("payment.manager.supplier.bank.start", purchaseOrderId, bankAccountId));

        validateManagerRole(managerId);

        BankAccountDto bankAccount = bankAccountService.getBankAccountById(bankAccountId);
        if (!bankAccount.getActive()) {
            throw new IllegalStateException(messageService.get("bank.account.inactive", bankAccountId));
        }
        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(bankAccountId, bankAccount.getBalance(), amount);
        }

        List<InvoiceDto> invoices = invoiceService.getInvoicesByPurchaseOrder(purchaseOrderId);
        InvoiceDto invoice = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING || i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.no.payable.invoice.purchase", purchaseOrderId)));

        bankAccountService.withdraw(
                BankAccountTransactionRequest.builder()
                        .accountId(bankAccountId)
                        .amount(amount)
                        .description(messageService.get("payment.supplier.payment.description", purchaseOrderId))
                        .build(),
                managerId
        );

        PaymentTransaction transaction = createSupplierPaymentTransaction(invoice, amount, BANK_ACCOUNT_TYPE, bankAccountId.toString());
        completeTransaction(transaction, PaymentResult.success(BANK_TXN_PREFIX + System.currentTimeMillis()));

        if (amount.compareTo(invoice.getRemainingAmount()) >= 0) {
            invoiceService.markAsPaid(invoice.getId(), null, managerId);
        } else {
            invoiceService.markAsPartiallyPaid(invoice.getId(), amount, null, managerId);
        }

        log.info(messageService.get("payment.manager.supplier.bank.success", purchaseOrderId, amount));
        return paymentTransactionConverter.toDto(transaction);
    }

    /**
     * Manager pays supplier from cash register.
     *
     * @param request the payment request with purchase order ID and cash register ID
     * @param managerId the ID of the manager
     * @return completed payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerPaySupplierFromCash(PaymentProcessRequest request, Long managerId) {
        Long purchaseOrderId = request.getPurchaseOrderId();
        Long cashRegisterId = request.getCashRegisterId();
        BigDecimal amount = request.getAmount();

        log.info(messageService.get("payment.manager.supplier.cash.start", purchaseOrderId, cashRegisterId));

        validateManagerRole(managerId);

        CashRegisterDto cashRegister = cashRegisterService.getCashRegisterById(cashRegisterId);
        if (!cashRegister.getIsActive()) {
            throw new IllegalStateException(messageService.get("cash.register.closed", cashRegisterId));
        }

        List<InvoiceDto> invoices = invoiceService.getInvoicesByPurchaseOrder(purchaseOrderId);
        InvoiceDto invoice = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING || i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.no.payable.invoice.purchase", purchaseOrderId)));

        CashTransactionRequest expenseRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.EXPENSE)
                .amount(amount)
                .invoiceId(invoice.getId())
                .purchaseOrderId(purchaseOrderId)
                .description(messageService.get("payment.supplier.payment.description", purchaseOrderId))
                .build();
        cashTransactionService.createTransaction(expenseRequest, managerId);

        PaymentTransaction transaction = createSupplierPaymentTransaction(invoice, amount, CASH_REGISTER_TYPE, cashRegisterId.toString());
        completeTransaction(transaction, PaymentResult.success(CASH_REGISTER_TXN_PREFIX + System.currentTimeMillis()));

        if (amount.compareTo(invoice.getRemainingAmount()) >= 0) {
            invoiceService.markAsPaid(invoice.getId(), cashRegisterId, managerId);
        } else {
            invoiceService.markAsPartiallyPaid(invoice.getId(), amount, cashRegisterId, managerId);
        }

        log.info(messageService.get("payment.manager.supplier.cash.success", purchaseOrderId, amount));
        return paymentTransactionConverter.toDto(transaction);
    }

    /**
     * Manager receives cash payment from customer.
     *
     * @param request the payment request with invoice ID, customer ID and cash register ID
     * @param managerId the ID of the manager
     * @return completed payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerReceiveCashPayment(PaymentProcessRequest request, Long managerId) {
        Long cashRegisterId = request.getCashRegisterId();
        Long invoiceId = request.getInvoiceId();
        BigDecimal amount = request.getAmount();

        log.info(messageService.get("payment.manager.receive.cash.start", invoiceId, amount));

        validateManagerRole(managerId);

        CashRegisterDto cashRegister = cashRegisterService.getCashRegisterById(cashRegisterId);
        if (!cashRegister.getIsActive()) {
            throw new IllegalStateException(messageService.get("cash.register.closed", cashRegisterId));
        }

        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException(messageService.get("invoice.already.paid", invoice.getInvoiceNumber()));
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException(messageService.get("invoice.cancelled", invoice.getInvoiceNumber()));
        }

        CashTransactionRequest incomeRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.INCOME)
                .amount(amount)
                .invoiceId(invoiceId)
                .customerId(request.getCustomerId())
                .salesOrderId(invoice.getSalesOrderId())
                .description(messageService.get("payment.customer.cash.payment.description",
                        invoice.getSalesOrderId(), request.getCustomerId()))
                .build();
        cashTransactionService.createTransaction(incomeRequest, managerId);

        PaymentTransaction transaction = createCustomerPaymentTransaction(invoice, amount, cashRegisterId.toString(), request.getCustomerId());
        completeTransaction(transaction, PaymentResult.success(CASH_REGISTER_TXN_PREFIX + System.currentTimeMillis()));

        if (amount.compareTo(invoice.getRemainingAmount()) >= 0) {
            invoiceService.markAsPaid(invoice.getId(), cashRegisterId, managerId);
        } else {
            invoiceService.markAsPartiallyPaid(invoice.getId(), amount, cashRegisterId, managerId);
        }

        log.info(messageService.get("payment.manager.receive.cash.success", invoiceId, amount));
        return paymentTransactionConverter.toDto(transaction);
    }

    /**
     * Manager processes cash refund to customer.
     *
     * @param request the payment request with original transaction ID and refund reason
     * @param managerId the ID of the manager
     * @return refund payment transaction DTO
     */
    @Transactional
    public PaymentTransactionDto managerProcessCashRefund(PaymentProcessRequest request, Long managerId) {
        Long originalTransactionId = request.getOriginalTransactionId();
        Long cashRegisterId = request.getCashRegisterId();
        BigDecimal amount = request.getAmount();
        String reason = request.getRefundReason();

        log.info(messageService.get("payment.manager.refund.cash.start", originalTransactionId, amount));

        validateManagerRole(managerId);

        CashRegisterDto cashRegister = cashRegisterService.getCashRegisterById(cashRegisterId);
        if (!cashRegister.getIsActive()) {
            throw new IllegalStateException(messageService.get("cash.register.closed", cashRegisterId));
        }

        PaymentTransaction originalTransaction = paymentTransactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.transaction.not.found", originalTransactionId)));

        if (originalTransaction.getStatus() != PaymentTransactionStatus.COMPLETED) {
            throw new IllegalStateException(messageService.get("payment.refund.non.completed"));
        }

        boolean alreadyRefunded = paymentTransactionRepository.existsByOriginalTransactionId(originalTransactionId);
        if (alreadyRefunded) {
            throw new IllegalStateException(messageService.get("payment.refund.already.processed"));
        }

        InvoiceDto invoice = invoiceService.getInvoiceById(originalTransaction.getInvoiceId());

        CashTransactionRequest refundRequest = CashTransactionRequest.builder()
                .cashRegisterId(cashRegisterId)
                .transactionType(TransactionType.EXPENSE)
                .amount(amount)
                .originalTransactionId(originalTransactionId)
                .invoiceId(invoice.getId())
                .description(messageService.get("payment.cash.refund.description", originalTransactionId, reason))
                .build();
        cashTransactionService.createTransaction(refundRequest, managerId);

        PaymentTransaction refundTransaction = createRefundTransaction(originalTransaction, amount, reason);
        refundTransaction = paymentTransactionRepository.save(refundTransaction);

        originalTransaction.setStatus(PaymentTransactionStatus.REFUNDED);
        paymentTransactionRepository.save(originalTransaction);

        log.info(messageService.get("payment.manager.refund.cash.success", originalTransactionId));
        return paymentTransactionConverter.toDto(refundTransaction);
    }

    // =========================================================================
    // QUERY METHODS
    // =========================================================================

    /**
     * Gets all supplier payments for a purchase order.
     *
     * @param purchaseOrderId the purchase order ID
     * @return list of payment transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getSupplierPayments(Long purchaseOrderId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByOrderIdAndOrderType(purchaseOrderId, OrderType.PURCHASE);
        return paymentTransactionConverter.toDtoList(transactions);
    }

    /**
     * Gets all customer payments for a sales order.
     *
     * @param salesOrderId the sales order ID
     * @return list of payment transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getCustomerPayments(Long salesOrderId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByOrderIdAndOrderType(salesOrderId, OrderType.SALES);
        return paymentTransactionConverter.toDtoList(transactions);
    }

    /**
     * Gets a payment transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return payment transaction DTO
     * @throws IllegalArgumentException if transaction not found
     */
    @Transactional(readOnly = true)
    public PaymentTransactionDto getPaymentTransaction(Long transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.transaction.not.found", transactionId)));
        return paymentTransactionConverter.toDto(transaction);
    }

    // =========================================================================
    // LEGACY METHODS (deprecated)
    // =========================================================================

    /**
     * Legacy method for processing payments.
     *
     * @deprecated Use {@link #processPayment(PaymentProcessRequest, Long)} instead
     */
    @Deprecated
    @Transactional
    public PaymentTransaction processPayment(Long paymentMethodId, BigDecimal amount, String currency,
                                             Long invoiceId, Long orderId, OrderType orderType, String description) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId).orElseThrow();
        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);
        PaymentTransaction transaction = createPaymentTransaction(paymentMethod, invoice, amount, null);
        transaction = paymentTransactionRepository.save(transaction);
        return transaction;
    }

    /**
     * Legacy method for refunding payments.
     *
     * @deprecated Use {@link #processPayment(PaymentProcessRequest, Long)} with refund fields instead
     */
    @Deprecated
    @Transactional
    public PaymentTransaction refundPayment(Long transactionId, String reason) {
        return null;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void validateCommonPaymentRequest(PaymentProcessRequest request) {
        if (request.getPaymentMethodId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.validation.payment.method.required"));
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(messageService.get("payment.validation.amount.positive"));
        }
    }

    private void validateManagerRole(Long userId) {
        SecurityUser securityUser = securityUserRepository.findById(userId)
                .orElseThrow(() -> new SecurityException(messageService.get("payment.user.not.found", userId)));
        if (securityUser.getRole() != Role.ADMIN && securityUser.getRole() != Role.MANAGER) {
            throw new SecurityException(messageService.get("payment.manager.role.required.message"));
        }
    }

    private boolean isManager(Long userId) {
        return securityUserRepository.findById(userId)
                .map(su -> su.getRole() == Role.ADMIN || su.getRole() == Role.MANAGER)
                .orElse(false);
    }

    private PaymentMethod validatePaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("payment.method.not.found", paymentMethodId)));

        if (!paymentMethod.isActive()) {
            throw new IllegalStateException(messageService.get("payment.method.inactive", paymentMethodId));
        }

        UserTypeAssignmentDto userType = userTypeAssignmentService.getCurrentUserType(userId);
        if (userType == null) {
            throw new IllegalStateException(messageService.get("user.type.not.found", userId));
        }

        boolean isAvailable = paymentMethodUserTypeRepository
                .existsByPaymentMethodIdAndUserType(paymentMethodId, userType.getUserType());

        if (!isAvailable) {
            throw new SecurityException(messageService.get("payment.method.not.available.for.user.type",
                    paymentMethodId, userType.getUserType()));
        }

        return paymentMethod;
    }

    private PaymentTransaction createPaymentTransaction(PaymentMethod paymentMethod, InvoiceDto invoice,
                                                        BigDecimal amount, Long userId) {
        return PaymentTransaction.builder()
                .paymentMethodId(paymentMethod.getId())
                .invoiceId(invoice.getId())
                .orderId(invoice.getPurchaseOrderId() != null ? invoice.getPurchaseOrderId() : invoice.getSalesOrderId())
                .orderType(invoice.getPurchaseOrderId() != null ? OrderType.PURCHASE : OrderType.SALES)
                .amount(amount)
                .currency(invoice.getCurrency())
                .status(PaymentTransactionStatus.PENDING)
                .description(messageService.get("payment.transaction.description",
                        invoice.getPurchaseOrderId() != null ? "purchase" : "sales",
                        invoice.getPurchaseOrderId() != null ? invoice.getPurchaseOrderId() : invoice.getSalesOrderId()))
                .processingFee(paymentMethod.getProcessingFee())
                .netAmount(calculateNetAmount(amount, paymentMethod.getProcessingFee()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentTransaction createSupplierPaymentTransaction(InvoiceDto invoice, BigDecimal amount,
                                                                String sourceType, String sourceId) {
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

    private PaymentTransaction createCustomerPaymentTransaction(InvoiceDto invoice, BigDecimal amount,
                                                                String paymentId, Long customerId) {
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

    private PaymentTransaction createRefundTransaction(PaymentTransaction original, BigDecimal amount, String reason) {
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

        paymentTransactionRepository.save(transaction);
    }

    private void failTransaction(PaymentTransaction transaction, String errorMessage) {
        transaction.setStatus(PaymentTransactionStatus.FAILED);
        transaction.setErrorMessage(errorMessage);
        paymentTransactionRepository.save(transaction);
    }

    private BigDecimal calculateNetAmount(BigDecimal amount, BigDecimal feePercent) {
        if (feePercent == null || feePercent.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        BigDecimal fee = amount.multiply(feePercent).divide(BigDecimal.valueOf(100));
        return amount.subtract(fee);
    }
}