package ru.galtor85.household_store.service.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderNotFoundException;
import ru.galtor85.household_store.advice.exception.order.SalesOrderNotFoundException;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.converter.InvoiceConverter;
import ru.galtor85.household_store.dto.request.finance.InvoiceCreateRequest;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.dto.response.finance.InvoiceStatisticsDto;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.processor.invoice.InvoicePaymentProcessor;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.currency.CurrencyConversionService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.generator.NumberGenerator;
import ru.galtor85.household_store.validator.finance.InvoiceValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.PaginationConstants.DESC_SORT_DIRECTION;

/**
 * Service for managing invoices
 * Handles invoice creation, retrieval, payment, and cancellation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceConverter invoiceConverter;
    private final InvoiceValidator invoiceValidator;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;
    private final InvoicePaymentProcessor paymentProcessor;
    private final CashTransactionRepository cashTransactionRepository;
    private final CurrencyConversionService currencyConversionService;
    private final FinancialConfig financialConfig;  // ← ДОБАВИТЬ

    // =========================================================================
    // INVOICE CREATION
    // =========================================================================

    /**
     * Creates a new invoice
     *
     * @param request   the invoice creation request
     * @param createdBy ID of the user creating the invoice
     * @return created invoice DTO
     */
    @Transactional
    public InvoiceDto createInvoice(InvoiceCreateRequest request, Long createdBy) {
        log.info(messageService.get("invoice.create.start",
                request.getPurchaseOrderId(), request.getSalesOrderId()));

        // Validate request
        invoiceValidator.validateCreateRequest(request);

        // Get amount in base currency for consistent storage
        BigDecimal amountInBaseCurrency = currencyConversionService.convertToBaseCurrency(
                request.getAmount(), request.getNormalizedCurrency());

        // Validate order existence
        if (request.getPurchaseOrderId() != null) {
            validatePurchaseOrder(request.getPurchaseOrderId());
        } else {
            validateSalesOrder(request.getSalesOrderId());
        }

        // Calculate due date based on order type
        LocalDateTime dueDate = request.getDueDate();
        if (dueDate == null) {
            dueDate = calculateDueDate(request);
        }

        // Build invoice entity
        Invoice invoice = Invoice.builder()
                .invoiceNumber(numberGenerator.generateInvoiceNumber())
                .purchaseOrderId(request.getPurchaseOrderId())
                .salesOrderId(request.getSalesOrderId())
                .amount(amountInBaseCurrency)
                .currency(request.getNormalizedCurrency())
                .status(InvoiceStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .issueDate(LocalDateTime.now())
                .dueDate(dueDate)
                .description(request.getDescription())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.created", saved.getInvoiceNumber()));

        // New invoice has zero payments
        return invoiceConverter.toDto(saved, BigDecimal.ZERO);
    }

    /**
     * Calculates due date based on order type and configuration.
     *
     * @param request the invoice creation request
     * @return calculated due date
     */
    private LocalDateTime calculateDueDate(InvoiceCreateRequest request) {
        if (request.getPurchaseOrderId() != null) {
            // Purchase order invoice
            Integer days = financialConfig.getInvoice().getPurchaseDueDays();
            int dueDays = days != null ? days : 30;
            return LocalDateTime.now().plusDays(dueDays);
        } else {
            // Sales order invoice - determine retail vs wholesale
            // Note: This requires additional logic to check order type
            // For now, use retail days as default
            Integer days = financialConfig.getInvoice().getRetailDueDays();
            int dueDays = days != null ? days : 7;
            return LocalDateTime.now().plusDays(dueDays);
        }
    }

    // =========================================================================
    // INVOICE RETRIEVAL
    // =========================================================================

    /**
     * Retrieves invoice by ID with payment information
     *
     * @param invoiceId invoice identifier
     * @return invoice DTO with payment details
     */
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceValidator.validateInvoiceExists(invoiceId);
        BigDecimal totalPaid = calculateTotalPaid(invoice);
        return invoiceConverter.toDto(invoice, totalPaid);
    }

    /**
     * Retrieves invoice by invoice number
     *
     * @param invoiceNumber unique invoice number
     * @return invoice DTO with payment details
     */
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
        BigDecimal totalPaid = calculateTotalPaid(invoice);
        return invoiceConverter.toDto(invoice, totalPaid);
    }

    /**
     * Retrieves paginated list of all invoices
     *
     * @param page    page number (0-indexed)
     * @param size    page size
     * @param sortBy  field to sort by
     * @param sortDir sort direction (asc/desc)
     * @return page of invoice DTOs
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDto> getAllInvoices(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(DESC_SORT_DIRECTION) ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);
        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoicePage.getContent());

        return invoicePage.map(invoice -> {
            BigDecimal totalPaid = paidAmounts.get(invoice.getId());
            return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
        });
    }

    /**
     * Retrieves invoices by status with pagination
     *
     * @param status invoice status
     * @param page   page number
     * @param size   page size
     * @return page of invoice DTOs
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDto> getInvoicesByStatus(InvoiceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("issueDate").descending());
        Page<Invoice> invoicePage = invoiceRepository.findByStatus(status, pageable);

        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoicePage.getContent());

        return invoicePage.map(invoice -> {
            BigDecimal totalPaid = paidAmounts.get(invoice.getId());
            return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
        });
    }

    /**
     * Retrieves all invoices for a purchase order
     *
     * @param purchaseOrderId purchase order identifier
     * @return list of invoice DTOs
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesByPurchaseOrder(Long purchaseOrderId) {
        validatePurchaseOrder(purchaseOrderId);
        List<Invoice> invoices = invoiceRepository.findByPurchaseOrderId(purchaseOrderId);

        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoices);

        return invoices.stream()
                .map(invoice -> {
                    BigDecimal totalPaid = paidAmounts.get(invoice.getId());
                    return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all invoices for a sales order
     *
     * @param salesOrderId sales order identifier
     * @return list of invoice DTOs
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesBySalesOrder(Long salesOrderId) {
        validateSalesOrder(salesOrderId);
        List<Invoice> invoices = invoiceRepository.findBySalesOrderId(salesOrderId);

        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoices);

        return invoices.stream()
                .map(invoice -> {
                    BigDecimal totalPaid = paidAmounts.get(invoice.getId());
                    return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // INVOICE PAYMENT
    // =========================================================================

    /**
     * Marks invoice as fully paid (legacy method using cash register)
     *
     * @param invoiceId      invoice identifier
     * @param cashRegisterId cash register used for payment
     * @param cashierId      ID of cashier processing payment
     * @return updated invoice DTO
     */
    @Transactional
    public InvoiceDto markAsPaid(Long invoiceId, Long cashRegisterId, Long cashierId) {
        InvoicePaymentProcessor.InvoicePaymentResult result = paymentProcessor.processPayment(
                invoiceId,
                getInvoiceAmount(invoiceId),
                cashRegisterId,
                cashierId,
                false
        );

        log.info(messageService.get("invoice.paid", result.getInvoice().getInvoiceNumber()));

        BigDecimal totalPaid = calculateTotalPaid(result.getInvoice());
        return invoiceConverter.toDto(result.getInvoice(), totalPaid);
    }

    /**
     * Marks invoice as partially paid (legacy method using cash register)
     *
     * @param invoiceId      invoice identifier
     * @param paidAmount     amount being paid
     * @param cashRegisterId cash register used for payment
     * @param cashierId      ID of cashier processing payment
     * @return updated invoice DTO
     */
    @Transactional
    public InvoiceDto markAsPartiallyPaid(Long invoiceId, BigDecimal paidAmount, Long cashRegisterId, Long cashierId) {
        InvoicePaymentProcessor.InvoicePaymentResult result = paymentProcessor.processPayment(
                invoiceId, paidAmount, cashRegisterId, cashierId, true
        );

        log.info(messageService.get("invoice.partially.paid",
                result.getInvoice().getInvoiceNumber(), paidAmount));

        BigDecimal totalPaid = calculateTotalPaid(result.getInvoice());
        return invoiceConverter.toDto(result.getInvoice(), totalPaid);
    }

    // =========================================================================
    // INVOICE CANCELLATION
    // =========================================================================

    /**
     * Cancels an invoice
     *
     * @param invoiceId   invoice identifier
     * @param reason      reason for cancellation
     * @param cancelledBy ID of user cancelling the invoice
     * @return cancelled invoice DTO
     */
    @Transactional
    public InvoiceDto cancelInvoice(Long invoiceId, String reason, Long cancelledBy) {
        log.info(messageService.get("invoice.cancel.start", invoiceId, reason));

        Invoice invoice = invoiceValidator.validateInvoiceExists(invoiceId);
        invoiceValidator.validateInvoiceCancellable(invoice);

        invoice.setStatus(InvoiceStatus.CANCELLED);
        String cancelNote = String.format("Cancelled: %s (user %d)", reason, cancelledBy);
        if (invoice.getNotes() == null) {
            invoice.setNotes(cancelNote);
        } else {
            invoice.setNotes(invoice.getNotes() + "\n" + cancelNote);
        }

        Invoice saved = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.cancelled", saved.getInvoiceNumber()));

        BigDecimal totalPaid = calculateTotalPaid(saved);
        return invoiceConverter.toDto(saved, totalPaid);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Gets total amount of pending invoices
     *
     * @return total pending amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingAmount() {
        return invoiceRepository.getTotalPendingAmount();
    }

    /**
     * Gets total paid amount for a time period
     *
     * @param startDate period start
     * @param endDate   period end
     * @return total paid amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmountForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.getTotalPaidAmountForPeriod(startDate, endDate);
    }

    /**
     * Gets total amount grouped by invoice status
     *
     * @return map of status to total amount
     */
    @Transactional(readOnly = true)
    public Map<InvoiceStatus, BigDecimal> getTotalAmountByStatus() {
        List<Object[]> results = invoiceRepository.getTotalAmountByStatus();
        Map<InvoiceStatus, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            InvoiceStatus status = (InvoiceStatus) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(status, amount);
        }
        return map;
    }

    /**
     * Gets total amount grouped by payment method
     *
     * @return map of payment method to total amount
     */
    @Transactional(readOnly = true)
    public Map<PaymentMethod, BigDecimal> getTotalAmountByPaymentMethod() {
        List<Object[]> results = invoiceRepository.getTotalAmountByPaymentMethod();
        Map<PaymentMethod, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(method, amount);
        }
        return map;
    }

    /**
     * Gets total pending amounts split by order type (purchase vs sales)
     *
     * @return InvoiceStatisticsDto with pending totals
     */
    @Transactional(readOnly = true)
    public InvoiceStatisticsDto getPendingAmountsByType() {
        BigDecimal purchaseTotal = invoiceRepository.getTotalPendingAmountForPurchase();
        BigDecimal salesTotal = invoiceRepository.getTotalPendingAmountForSales();

        purchaseTotal = purchaseTotal != null ? purchaseTotal : BigDecimal.ZERO;
        salesTotal = salesTotal != null ? salesTotal : BigDecimal.ZERO;

        BigDecimal total = purchaseTotal.add(salesTotal);

        return InvoiceStatisticsDto.builder()
                .purchasePendingTotal(purchaseTotal)
                .salesPendingTotal(salesTotal)
                .totalPending(total)
                .build();
    }

    /**
     * Gets total pending amount for purchase orders only
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingAmountForPurchase() {
        BigDecimal amount = invoiceRepository.getTotalPendingAmountForPurchase();
        return amount != null ? amount : BigDecimal.ZERO;
    }

    /**
     * Gets total pending amount for sales orders only
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingAmountForSales() {
        BigDecimal amount = invoiceRepository.getTotalPendingAmountForSales();
        return amount != null ? amount : BigDecimal.ZERO;
    }

    // =========================================================================
    // OVERDUE INVOICES
    // =========================================================================

    /**
     * Gets all overdue invoices
     *
     * @return list of overdue invoice DTOs
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getOverdueInvoices() {
        List<Invoice> invoices = invoiceRepository.findOverdueInvoices(LocalDateTime.now());
        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoices);

        return invoices.stream()
                .map(invoice -> {
                    BigDecimal totalPaid = paidAmounts.get(invoice.getId());
                    return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets invoices due in the next N days
     *
     * @param days number of days to look ahead (from configuration or request)
     * @return list of upcoming invoice DTOs
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getUpcomingInvoices(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(days);
        List<Invoice> invoices = invoiceRepository.findUpcomingInvoices(now, endDate);

        Map<Long, BigDecimal> paidAmounts = calculateTotalPaidMap(invoices);

        return invoices.stream()
                .map(invoice -> {
                    BigDecimal totalPaid = paidAmounts.get(invoice.getId());
                    return invoiceConverter.toDto(invoice, totalPaid != null ? totalPaid : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets default upcoming invoices based on configuration
     *
     * @return list of upcoming invoice DTOs using default days from config
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getDefaultUpcomingInvoices() {
        Integer defaultDays = financialConfig.getInvoice().getRetailDueDays();
        int days = defaultDays != null ? defaultDays : 7;
        return getUpcomingInvoices(days);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Gets invoice amount by ID
     *
     * @param invoiceId invoice identifier
     * @return invoice amount
     */
    private BigDecimal getInvoiceAmount(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(Invoice::getAmount)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    /**
     * Validates that a purchase order exists
     *
     * @param purchaseOrderId purchase order identifier
     */
    private void validatePurchaseOrder(Long purchaseOrderId) {
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new PurchaseOrderNotFoundException(purchaseOrderId);
        }
    }

    /**
     * Validates that a sales order exists
     *
     * @param salesOrderId sales order identifier
     */
    private void validateSalesOrder(Long salesOrderId) {
        if (!salesOrderRepository.existsById(salesOrderId)) {
            throw new SalesOrderNotFoundException(salesOrderId);
        }
    }

    /**
     * Calculates total paid amount for an invoice
     * Sums INCOME and REFUND transactions, subtracts EXPENSE transactions
     *
     * @param invoice the invoice entity
     * @return total paid amount
     */
    private BigDecimal calculateTotalPaid(Invoice invoice) {
        List<CashTransaction> transactions = cashTransactionRepository.findByInvoiceId(invoice.getId());

        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPaid = BigDecimal.ZERO;

        for (CashTransaction tx : transactions) {
            if (invoice.isPurchaseOrder()) {
                // For purchase orders: EXPENSE = payment to supplier
                if (tx.getTransactionType() == TransactionType.EXPENSE) {
                    totalPaid = totalPaid.add(tx.getAmount());
                }
                // REFUND = money back from supplier (decreases paid amount)
                else if (tx.getTransactionType() == TransactionType.REFUND) {
                    totalPaid = totalPaid.subtract(tx.getAmount());
                }
            }
            else if (invoice.isSalesOrder()) {
                // For sales orders: INCOME = payment from customer
                if (tx.getTransactionType() == TransactionType.INCOME) {
                    totalPaid = totalPaid.add(tx.getAmount());
                }
                // REFUND = money back to customer (decreases paid amount)
                else if (tx.getTransactionType() == TransactionType.REFUND) {
                    totalPaid = totalPaid.subtract(tx.getAmount());
                }
            }
        }

        return totalPaid;
    }

    /**
     * Calculates total paid amounts for multiple invoices
     *
     * @param invoices list of invoices
     * @return map of invoice ID to total paid amount
     */
    private Map<Long, BigDecimal> calculateTotalPaidMap(List<Invoice> invoices) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Invoice invoice : invoices) {
            result.put(invoice.getId(), calculateTotalPaid(invoice));
        }
        return result;
    }
}