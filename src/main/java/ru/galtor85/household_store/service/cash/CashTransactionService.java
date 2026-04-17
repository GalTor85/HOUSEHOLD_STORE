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
import ru.galtor85.household_store.converter.CashTransactionConverter;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.cash.CashTransactionProcessor;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.validator.cash.CashTransactionValidator;

import java.math.BigDecimal;
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

    // =========================================================================
    // CREATE TRANSACTION
    // =========================================================================

    /**
     * Creates a new cash transaction.
     *
     * @param request transaction request
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
     * @param startDate period start
     * @param endDate period end
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
     * @param reason cancellation reason
     * @param cashierId cashier ID
     * @return refund transaction DTO
     */
    @Transactional
    public CashTransactionDto cancelTransaction(Long transactionId, String reason, Long cashierId) {
        log.info(logMsg.get("cash.transaction.service.cancel.start", transactionId, reason));

        // Validate transaction exists and is refundable
        CashTransaction original = validator.validateRefundableTransactionExists(transactionId);

        validator.validateTransactionCancellable(original);

        validator.validateTransactionRefundable(original, original.getAmount());


        // Add cash register validation
        CashRegister cashRegister = cashRegisterService.validateCashRegisterExists(
                original.getCashRegister().getId());
        validator.validateCashRegisterActive(cashRegister);  // Already there, good!

       // Create refund transaction
        CashTransaction refundTransaction = processor.createRefundTransaction(original, reason, cashierId);

        // Update invoice status if needed
        if (original.getInvoice() != null) {
            updateInvoiceStatusAfterRefund(original.getInvoice());
        }

        log.info(logMsg.get("cash.transaction.service.cancelled",
                transactionId, refundTransaction.getId()));

        return converter.toDto(refundTransaction);
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

    private void updateInvoiceStatusAfterRefund(Invoice invoice) {
        updateInvoiceStatus(invoice);

        log.info(logMsg.get("invoice.status.updated.after.refund",
                invoice.getInvoiceNumber(),
                invoice.getStatus().name(),
                invoice.getTotalPaidAmount()));
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