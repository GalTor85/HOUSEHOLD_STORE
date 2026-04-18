package ru.galtor85.household_store.service.cash;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.converter.CashTransactionConverter;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.cash.CashTransactionProcessor;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.validator.cash.CashTransactionValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Service for managing cash transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CashTransactionValidator validator;
    private final CashTransactionProcessor processor;
    private final CashTransactionConverter converter;
    private final CashRegisterService cashRegisterService;
    private final UserSearchService userSearchService;
    private final LogMessageService logMsg;
    private final MessageService messageService;
    private final FinancialConfig financialConfig;

    private int getScale() {
        return financialConfig.getDefaultDecimalPlaces();
    }

    // =========================================================================
    // RECORD FOR PROPORTIONAL REFUND
    // =========================================================================

    /**
     * Result of proportional refund calculation for a single payment transaction.
     * Used internally within service and for testing.
     *
     * @param originalTransactionId ID of the original payment transaction
     * @param refundAmount calculated amount to refund from this transaction
     */
    public record ProportionalRefundItem(
            Long originalTransactionId,
            BigDecimal refundAmount
    ) {}

    // =========================================================================
    // PROPORTIONAL REFUND CALCULATION
    // =========================================================================

    /**
     * Calculates proportional refund amounts for an invoice based on partial payments.
     * Each payment receives refund amount proportional to its share of total paid.
     *
     * @param invoiceId ID of the invoice to refund
     * @param totalRefundAmount total amount to refund
     * @return list of ProportionalRefundItem for each payment
     * @throws InvoiceNotFoundException if invoice not found
     * @throws IllegalArgumentException if no payments found or refund exceeds paid amount
     */
    @Transactional(readOnly = true)
    public List<ProportionalRefundItem> calculateProportionalRefunds(Long invoiceId,
                                                                     BigDecimal totalRefundAmount) {

        log.info(logMsg.get("refund.calculation.start", invoiceId, totalRefundAmount));

        // 1. Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // 2. Determine payment type based on invoice type
        TransactionType paymentType = invoice.getPurchaseOrderId() != null
                ? TransactionType.EXPENSE
                : TransactionType.INCOME;

        // 3. Get all payments for this invoice
        List<CashTransaction> payments = cashTransactionRepository.findByInvoiceId(invoiceId)
                .stream()
                .filter(t -> t.getTransactionType() == paymentType)
                .toList();

        if (payments.isEmpty()) {
            log.warn(logMsg.get("refund.error.no.payments", invoice.getInvoiceNumber()));
            throw new IllegalArgumentException(
                    messageService.get("refund.error.no.payments", invoice.getInvoiceNumber())
            );
        }

        // 4. Calculate total paid amount
        BigDecimal totalPaid = payments.stream()
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug(logMsg.get("refund.calculation.total", totalPaid, payments.size()));

        // 5. Validate refund amount
        if (totalRefundAmount.compareTo(totalPaid) > 0) {
            log.warn(logMsg.get("refund.error.amount.exceeds",
                    totalRefundAmount, totalPaid, invoice.getInvoiceNumber()));
            throw new IllegalArgumentException(
                    messageService.get("refund.error.amount.exceeds.paid",
                            totalRefundAmount, totalPaid, invoice.getInvoiceNumber())
            );
        }

        // 6. Calculate proportional refunds
        List<ProportionalRefundItem> result = new ArrayList<>();
        BigDecimal remainingToRefund = totalRefundAmount;

        for (CashTransaction payment : payments) {
            if (remainingToRefund.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Calculate proportion: (payment_amount / total_paid) * refund_amount
            int scale = getScale();
            BigDecimal proportion = payment.getAmount()
                    .multiply(totalRefundAmount)
                    .divide(totalPaid, scale, RoundingMode.HALF_UP);

            BigDecimal refundForThisPayment = proportion.min(remainingToRefund);

            result.add(new ProportionalRefundItem(
                    payment.getId(),
                    refundForThisPayment
            ));

            remainingToRefund = remainingToRefund.subtract(refundForThisPayment);

            log.debug(logMsg.get("refund.calculation.item",
                    payment.getId(), refundForThisPayment, payment.getAmount()));
        }

        log.info(logMsg.get("refund.calculation.complete",
                invoice.getInvoiceNumber(), result.size(), totalRefundAmount));

        return result;
    }

    /**
     * Executes proportional refund for an invoice.
     * Calculates distribution across all payments and creates partial refunds.
     *
     * @param invoiceId ID of the invoice
     * @param totalRefundAmount total amount to refund
     * @param reason reason for refund
     * @param cashierId ID of the cashier performing the refund
     * @return list of created refund transactions
     */
    @Transactional
    public List<CashTransactionDto> executeProportionalRefund(Long invoiceId,
                                                              BigDecimal totalRefundAmount,
                                                              String reason,
                                                              Long cashierId) {

        if (totalRefundAmount == null || totalRefundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn(logMsg.get("refund.execute.proportional.invalid.amount", totalRefundAmount));
            throw new IllegalArgumentException(
                    messageService.get("refund.error.invalid.amount", totalRefundAmount)
            );
        }

        log.info(logMsg.get("refund.execute.proportional.start", invoiceId, totalRefundAmount, reason));

        // 1. Calculate proportional distribution across all payments
        List<ProportionalRefundItem> items = calculateProportionalRefunds(invoiceId, totalRefundAmount);

        if (items.isEmpty()) {
            log.warn(logMsg.get("refund.execute.proportional.no.items", invoiceId));
            return Collections.emptyList();
        }

        // 2. Execute each partial refund using existing method
        List<CashTransactionDto> refunds = new ArrayList<>();
        for (ProportionalRefundItem item : items) {
            CashTransactionDto refund = createPartialRefund(
                    item.originalTransactionId(),
                    item.refundAmount(),
                    reason,
                    cashierId
            );
            refunds.add(refund);

            log.debug(logMsg.get("refund.execute.proportional.item",
                    item.originalTransactionId(), item.refundAmount()));
        }

        log.info(logMsg.get("refund.execute.proportional.complete",
                invoiceId, refunds.size(), totalRefundAmount));

        return refunds;
    }

    // =========================================================================
    // CREATE TRANSACTION
    // =========================================================================

    /**
     * Creates a new cash transaction.
     *
     * @param request   transaction request
     * @param cashierId cashier ID
     * @return created transaction DTO
     */
    // CashTransactionService.java
    @Transactional
    public CashTransactionDto createTransaction(CashTransactionRequest request, Long cashierId) {
        log.info(logMsg.get("cash.transaction.service.create.start",
                request.getTransactionType(), request.getAmount()));

        validator.validateRequest(request);

        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(request.getCashRegisterId());
        validator.validateCashRegisterActive(cashRegister);

        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElseThrow(() -> new InvoiceNotFoundException(request.getInvoiceId()));

            if (request.getTransactionType() == TransactionType.REFUND) {
                validator.validateInvoiceForRefund(invoice, request.getAmount());
            } else {
                validator.validateInvoiceForPayment(invoice, request.getAmount());
            }
        }

        if (request.getTransactionType() == TransactionType.EXPENSE) {
            BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegister.getId());
            validator.validateSufficientBalance(cashRegister, request.getAmount(), currentBalance);
        }

        CashTransaction transaction = processor.createTransaction(request, cashRegister, invoice, cashierId);

        // Update invoice status for all transaction types
        if (invoice != null) {
            if (request.getTransactionType() == TransactionType.REFUND) {
                updateInvoiceStatusAfterRefund(invoice);
            } else {
                updateInvoiceStatus(invoice);
            }
        }

        return converter.toDto(transaction);
    }

    /**
     * Gets all transactions for an invoice with sequential balances.
     *
     * @param invoiceId invoice ID
     * @return list of transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<CashTransactionDto> getTransactionsByInvoice(Long invoiceId) {
        List<CashTransaction> transactions = cashTransactionRepository.findByInvoiceIdOrdered(invoiceId);

        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<CashTransactionDto> result = new ArrayList<>();
        for (CashTransaction tx : transactions) {
            CashTransactionDto dto = converter.toDtoWithBalance(
                    tx, tx.getBalanceBefore(), tx.getBalanceAfter());
            enrichDtoWithDetails(dto, tx);
            result.add(dto);
        }
        return result;
    }

    /**
     * Gets transactions within a date period.
     *
     * @param startDate      period start
     * @param endDate        period end
     * @param cashRegisterId optional cash register filter
     * @return list of transaction DTOs
     */
    @Transactional(readOnly = true)
    public List<CashTransactionDto> getTransactionsByPeriod(LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Long cashRegisterId) {
        List<CashTransaction> transactions;
        if (cashRegisterId != null) {
            transactions = cashTransactionRepository.findByCashRegisterIdAndDateRange(
                    cashRegisterId, startDate, endDate);
        } else {
            transactions = cashTransactionRepository.findAll().stream()
                    .filter(t -> t.getCreatedAt().isAfter(startDate) &&
                            t.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
        }
        return transactions.stream().map(this::enrichWithDetails).collect(Collectors.toList());
    }

    // =========================================================================
    // CANCEL TRANSACTION
    // =========================================================================

    /**
     * Cancels a cash transaction (creates a refund transaction).
     *
     * @param transactionId transaction ID
     * @param reason        cancellation reason
     * @param cashierId     cashier ID
     * @return refund transaction DTO
     */
    @Transactional
    public CashTransactionDto cancelTransaction(Long transactionId, String reason, Long cashierId) {
        log.info(logMsg.get("cash.transaction.service.cancel.start", transactionId, reason));

        CashTransaction original = validator.validateRefundableTransactionExists(transactionId);
        validator.validateTransactionCancellable(original);
        validator.validateTransactionRefundable(original, original.getAmount());

        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(
                original.getCashRegister().getId());
        validator.validateCashRegisterActive(cashRegister);

        CashTransaction refundTransaction = processor.createRefundTransaction(original, reason, cashierId);

        if (original.getInvoice() != null) {
            updateInvoiceStatusAfterRefund(original.getInvoice());
        }

        log.info(logMsg.get("cash.transaction.service.cancelled",
                transactionId, refundTransaction.getId()));

        return converter.toDtoWithDetails(
                refundTransaction,
                cashRegister,
                original.getInvoice(),
                null,
                refundTransaction.getBalanceBefore()  // используем баланс из транзакции
        );
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private CashTransactionDto enrichWithDetails(CashTransaction transaction) {
        CashRegister cashRegister = transaction.getCashRegister();
        Invoice invoice = transaction.getInvoice();
        User cashier = transaction.getCashierId() != null ?
                userSearchService.getUserById(transaction.getCashierId()) : null;
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        return converter.toDtoWithDetails(transaction, cashRegister, invoice, cashier, balanceBefore);
    }

    private void updateInvoiceStatus(Invoice invoice) {
        BigDecimal totalPaid = invoice.getTotalPaidAmount();

        InvoiceStatus newStatus;
        if (totalPaid.compareTo(invoice.getAmount()) >= 0) {
            newStatus = InvoiceStatus.PAID;
            invoice.setPaidDate(LocalDateTime.now());
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            newStatus = InvoiceStatus.PARTIALLY_PAID;
            invoice.setPaidDate(null);
        } else {
            newStatus = InvoiceStatus.PENDING;
            invoice.setPaidDate(null);
        }

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);

        log.info(logMsg.get("invoice.status.updated",
                invoice.getInvoiceNumber(),
                oldStatus != null ? oldStatus.name() : "null",
                newStatus.name(),
                totalPaid,
                invoice.getAmount()));
    }

    /**
     * Creates a partial refund for a transaction.
     *
     * @param originalTransactionId ID of the original transaction
     * @param refundAmount          amount to refund
     * @param reason                reason for refund
     * @param cashierId             ID of the cashier performing the refund
     * @return DTO of the created refund transaction with current balance
     */
    @Transactional
    public CashTransactionDto createPartialRefund(Long originalTransactionId,
                                                  BigDecimal refundAmount,
                                                  String reason,
                                                  Long cashierId) {
        log.info(logMsg.get("cash.transaction.service.partial.refund.start",
                originalTransactionId, refundAmount));

        // Validate transaction exists and is refundable
        CashTransaction original = validator.validateRefundableTransactionExists(originalTransactionId);
        validator.validateTransactionCancellable(original);
        validator.validateTransactionRefundable(original, refundAmount);

        // Validate cash register is active
        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(
                original.getCashRegister().getId());
        validator.validateCashRegisterActive(cashRegister);

        // Create partial refund transaction
        CashTransaction refundTransaction = processor.createPartialRefundTransaction(
                original, refundAmount, reason, cashierId);

        // Update invoice status if needed
        if (original.getInvoice() != null) {
            updateInvoiceStatusAfterRefund(original.getInvoice());
        }

        log.info(logMsg.get("cash.transaction.service.partial.refund.complete",
                originalTransactionId, refundTransaction.getId(), refundAmount));

        // Build response DTO with current balance
        CashTransactionDto result = converter.toDto(refundTransaction);
        BigDecimal currentBalance = cashRegisterService.getCurrentBalance(cashRegister.getId());
        result.setBalanceAfter(currentBalance);

        return result;
    }

    private void updateInvoiceStatusAfterRefund(Invoice invoice) {
        BigDecimal totalPaid = invoice.getTotalPaidAmount();

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {

            invoice.setStatus(InvoiceStatus.REFUNDED);
            invoice.setPaidDate(null);
        } else if (totalPaid.compareTo(invoice.getAmount()) < 0) {

            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {

            invoice.setStatus(InvoiceStatus.PAID);
        }

        invoiceRepository.save(invoice);

        log.info(logMsg.get("invoice.status.updated.after.refund",
                invoice.getInvoiceNumber(),
                invoice.getStatus().name(),
                totalPaid));
    }

    /**
     * Gets transaction by ID with full details.
     *
     * @param transactionId transaction ID
     * @return transaction DTO with enriched details
     * @throws IllegalArgumentException if transaction not found
     */
    @Transactional(readOnly = true)
    public CashTransactionDto getTransactionById(Long transactionId) {
        log.debug(logMsg.get("cash.transaction.service.get.by.id", transactionId));

        CashTransaction transaction = validator.validateTransactionExists(transactionId);
        CashTransactionDto dto = converter.toDto(transaction);

        enrichDtoWithDetails(dto, transaction);

        return dto;
    }

    /**
     * Gets paginated transactions by cash register.
     *
     * @param cashRegisterId cash register ID
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy sort field
     * @param sortDir sort direction (asc/desc)
     * @return page of transaction DTOs
     */
    @Transactional(readOnly = true)
    public Page<CashTransactionDto> getTransactionsByCashRegister(Long cashRegisterId,
                                                                  int page,
                                                                  int size,
                                                                  String sortBy,
                                                                  String sortDir) {
        log.debug(logMsg.get("cash.transaction.service.get.by.register", cashRegisterId, page, size));

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CashTransaction> transactions = cashTransactionRepository
                .findByCashRegisterId(cashRegisterId, pageable);

        return transactions.map(transaction -> {
            CashTransactionDto dto = converter.toDto(transaction);
            enrichDtoWithDetails(dto, transaction);
            return dto;
        });
    }

    private void enrichDtoWithDetails(CashTransactionDto dto, CashTransaction tx) {
        CashRegister cr = tx.getCashRegister();
        Invoice inv = tx.getInvoice();
        User cashier = tx.getCashierId() != null ? userSearchService.getUserById(tx.getCashierId()) : null;

        dto.setCashRegisterId(cr.getId());
        dto.setCashRegisterName(cr.getName());
        dto.setCashRegisterNumber(cr.getRegisterNumber());

        if (inv != null) {
            dto.setInvoiceId(inv.getId());
            dto.setInvoiceNumber(inv.getInvoiceNumber());
        }

        if (cashier != null) {
            dto.setCashierId(cashier.getId());
            dto.setCashierName(cashier.getFirstName() + " " + cashier.getLastName());
            dto.setCashierEmail(cashier.getEmail());
        }
    }
}