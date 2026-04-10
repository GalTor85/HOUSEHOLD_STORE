package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.auth.CustomAuthenticationException;
import ru.galtor85.household_store.dto.response.finance.*;
import ru.galtor85.household_store.dto.request.finance.CashRegisterCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CashRegisterUpdateRequest;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.request.finance.InvoiceCreateRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.cash.CashTransactionService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.finance.InvoiceService;
import ru.galtor85.household_store.service.user.UserSearchService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_FINANCE;

/**
 * REST controller for finance operations including invoices, cash registers, and transactions.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Invoice management (create, retrieve, pay, cancel)</li>
 *   <li>Cash register management (create, open, close, update)</li>
 *   <li>Cash transaction management (create, retrieve, cancel)</li>
 *   <li>Financial statistics and reporting</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_FINANCE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Finance Operations", description = "Endpoints for managing invoices, cash registers and transactions")
public class FinanceController {

    private final InvoiceService invoiceService;
    private final CashRegisterService cashRegisterService;
    private final CashTransactionService cashTransactionService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("manager.error.not.authenticated"));
        }
        return (SecurityUser) auth.getPrincipal();
    }

    private User getCurrentUser() {
        SecurityUser securityUser = getCurrentSecurityUser();
        return userSearchService.getUserById(securityUser.getUserId());
    }

    // =========================================================================
    // INVOICE MANAGEMENT
    // =========================================================================

    /**
     * Creates a new invoice for a purchase or sales order.
     *
     * @param request invoice creation request with order ID, amount, payment method
     * @return created invoice DTO
     */
    @PostMapping("/invoices")
    @Operation(summary = "Create a new invoice",
            description = "Creates a new invoice for a purchase order or sales order")
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.createInvoice(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("invoice.created", invoice.getInvoiceNumber()),
                        invoice));
    }

    /**
     * Retrieves an invoice by its ID.
     *
     * @param invoiceId invoice identifier
     * @return invoice DTO
     */
    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice by ID",
            description = "Retrieves detailed information about an invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId) {

        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.fetched"),
                invoice));
    }

    /**
     * Retrieves a paginated list of all invoices.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction (asc/desc)
     * @return page of invoice DTOs
     */
    @GetMapping("/invoices")
    @Operation(summary = "Get all invoices with pagination",
            description = "Retrieves a paginated list of all invoices")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> getAllInvoices(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "issueDate")
            @RequestParam(defaultValue = "issueDate") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<InvoiceDto> invoices = invoiceService.getAllInvoices(page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.fetched"),
                invoices));
    }

    /**
     * Retrieves invoices by status with pagination.
     *
     * @param status invoice status filter
     * @param page page number
     * @param size page size
     * @return page of invoice DTOs
     */
    @GetMapping("/invoices/status/{status}")
    @Operation(summary = "Get invoices by status",
            description = "Retrieves invoices filtered by status")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> getInvoicesByStatus(
            @Parameter(description = "Invoice status", example = "PENDING", required = true,
                    schema = @Schema(allowableValues = {"PENDING", "PAID", "PARTIALLY_PAID", "CANCELLED", "REFUNDED"}))
            @PathVariable InvoiceStatus status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Page<InvoiceDto> invoices = invoiceService.getInvoicesByStatus(status, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.status.fetched",
                        status.getLocalizedName(messageService)),
                invoices));
    }

    /**
     * Retrieves invoices for a specific purchase order.
     *
     * @param orderId purchase order ID
     * @return list of invoice DTOs
     */
    @GetMapping("/invoices/purchase-order/{orderId}")
    @Operation(summary = "Get invoices by purchase order",
            description = "Retrieves all invoices associated with a purchase order")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesByPurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        List<InvoiceDto> invoices = invoiceService.getInvoicesByPurchaseOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.purchase.order.fetched", orderId),
                invoices));
    }

    /**
     * Retrieves invoices for a specific sales order.
     *
     * @param orderId sales order ID
     * @return list of invoice DTOs
     */
    @GetMapping("/invoices/sales-order/{orderId}")
    @Operation(summary = "Get invoices by sales order",
            description = "Retrieves all invoices associated with a sales order")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesBySalesOrder(
            @Parameter(description = "Sales order ID", example = "1", required = true)
            @PathVariable Long orderId) {

        List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.sales.order.fetched", orderId),
                invoices));
    }

    /**
     * Marks an invoice as fully paid.
     *
     * @param invoiceId invoice ID
     * @param cashRegisterId cash register used for payment
     * @return updated invoice DTO
     */
    @PutMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Mark invoice as paid",
            description = "Marks an invoice as fully paid and records the payment transaction")
    public ResponseEntity<ApiResponse<InvoiceDto>> markInvoiceAsPaid(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId,
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @RequestParam Long cashRegisterId) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.markAsPaid(invoiceId, cashRegisterId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.paid.success"),
                invoice));
    }

    /**
     * Partially pays an invoice.
     *
     * @param invoiceId invoice ID
     * @param amount amount to pay
     * @param cashRegisterId cash register used for payment
     * @return updated invoice DTO
     */
    @PutMapping("/invoices/{invoiceId}/partial-pay")
    @Operation(summary = "Partially pay invoice",
            description = "Records a partial payment against an invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> partiallyPayInvoice(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId,
            @Parameter(description = "Payment amount", example = "500.00", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @RequestParam Long cashRegisterId) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.markAsPartiallyPaid(invoiceId, amount,cashRegisterId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.partially.paid.success", amount),
                invoice));
    }

    /**
     * Cancels an invoice.
     *
     * @param invoiceId invoice ID
     * @param reason cancellation reason
     * @return updated invoice DTO
     */
    @PutMapping("/invoices/{invoiceId}/cancel")
    @Operation(summary = "Cancel invoice",
            description = "Cancels an existing invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancelInvoice(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId,
            @Parameter(description = "Cancellation reason", example = "Order cancelled", required = true)
            @RequestParam String reason) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.cancelInvoice(invoiceId, reason, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.cancelled.success"),
                invoice));
    }

    // =========================================================================
    // CASH REGISTER MANAGEMENT
    // =========================================================================

    /**
     * Creates a new cash register.
     *
     * @param request cash register creation request
     * @return created cash register DTO
     */
    @PostMapping("/cash-registers")
    @Operation(summary = "Create a new cash register",
            description = "Creates a new cash register in the system")
    public ResponseEntity<ApiResponse<CashRegisterDto>> createCashRegister(
            @Valid @RequestBody CashRegisterCreateRequest request) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.createCashRegister(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cash.register.created"),
                        cashRegister));
    }

    /**
     * Retrieves all cash registers.
     *
     * @return list of cash register DTOs
     */
    @GetMapping("/cash-registers")
    @Operation(summary = "Get all cash registers",
            description = "Retrieves a list of all cash registers")
    public ResponseEntity<ApiResponse<List<CashRegisterDto>>> getAllCashRegisters() {

        List<CashRegisterDto> cashRegisters = cashRegisterService.getAllCashRegisters();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.registers.fetched"),
                cashRegisters));
    }

    /**
     * Retrieves all active cash registers.
     *
     * @return list of active cash register DTOs
     */
    @GetMapping("/cash-registers/active")
    @Operation(summary = "Get all active cash registers",
            description = "Retrieves a list of all currently active cash registers")
    public ResponseEntity<ApiResponse<List<CashRegisterDto>>> getActiveCashRegisters() {

        List<CashRegisterDto> cashRegisters = cashRegisterService.getActiveCashRegisters();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.registers.active.fetched"),
                cashRegisters));
    }

    /**
     * Retrieves a cash register by its ID.
     *
     * @param cashRegisterId cash register ID
     * @return cash register DTO
     */
    @GetMapping("/cash-registers/{cashRegisterId}")
    @Operation(summary = "Get cash register by ID",
            description = "Retrieves detailed information about a cash register")
    public ResponseEntity<ApiResponse<CashRegisterDto>> getCashRegisterById(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId) {

        CashRegisterDto cashRegister = cashRegisterService.getCashRegisterById(cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.fetched"),
                cashRegister));
    }

    /**
     * Opens a cash register for a new shift.
     *
     * @param cashRegisterId cash register ID
     * @param openingBalance initial cash balance (optional)
     * @return updated cash register DTO
     */
    @PostMapping("/cash-registers/{cashRegisterId}/open")
    @Operation(summary = "Open cash register",
            description = "Opens a cash register for a new shift")
    public ResponseEntity<ApiResponse<CashRegisterDto>> openCashRegister(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId,
            @Parameter(description = "Opening balance", example = "10000.00")
            @RequestParam(required = false) BigDecimal openingBalance) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.openCashRegister(
                cashRegisterId, openingBalance, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.opened"),
                cashRegister));
    }

    /**
     * Closes a cash register at the end of a shift.
     *
     * @param cashRegisterId cash register ID
     * @param closingBalance actual cash count
     * @param discrepancyReason reason for any discrepancy (required if difference exists)
     * @return updated cash register DTO
     */
    @PostMapping("/cash-registers/{cashRegisterId}/close")
    @Operation(summary = "Close cash register",
            description = "Closes a cash register at the end of a shift")
    public ResponseEntity<ApiResponse<CashRegisterDto>> closeCashRegister(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId,
            @Parameter(description = "Closing balance (actual cash count)", example = "15000.00", required = true)
            @RequestParam BigDecimal closingBalance,
            @Parameter(description = "Reason for discrepancy (required if actual != expected)")
            @RequestParam(required = false) String discrepancyReason ) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.closeCashRegister(
                cashRegisterId, closingBalance, discrepancyReason, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.closed"),
                cashRegister));
    }

    /**
     * Updates a cash register's details.
     *
     * @param cashRegisterId cash register ID
     * @param request update request
     * @return updated cash register DTO
     */
    @PutMapping("/cash-registers/{cashRegisterId}")
    @Operation(summary = "Update cash register",
            description = "Updates cash register details (name, location)")
    public ResponseEntity<ApiResponse<CashRegisterDto>> updateCashRegister(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId,
            @Valid @RequestBody CashRegisterUpdateRequest request) {

        CashRegisterDto cashRegister = cashRegisterService.updateCashRegister(cashRegisterId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.updated"),
                cashRegister));
    }

    // =========================================================================
    // CASH TRANSACTION MANAGEMENT
    // =========================================================================

    /**
     * Creates a new cash transaction (income or expense).
     *
     * @param request cash transaction request
     * @return created transaction DTO
     */
    @PostMapping("/cash-transactions")
    @Operation(summary = "Create a cash transaction",
            description = "Creates a new cash transaction (income or expense)")
    public ResponseEntity<ApiResponse<CashTransactionDto>> createCashTransaction(
            @Valid @RequestBody CashTransactionRequest request) {

        User currentUser = getCurrentUser();
        CashTransactionDto transaction = cashTransactionService.createTransaction(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cash.transaction.created"),
                        transaction));
    }

    /**
     * Retrieves a cash transaction by its ID.
     *
     * @param transactionId transaction ID
     * @return transaction DTO
     */
    @GetMapping("/cash-transactions/{transactionId}")
    @Operation(summary = "Get cash transaction by ID",
            description = "Retrieves detailed information about a cash transaction")
    public ResponseEntity<ApiResponse<CashTransactionDto>> getCashTransactionById(
            @Parameter(description = "Transaction ID", example = "1", required = true)
            @PathVariable Long transactionId) {

        CashTransactionDto transaction = cashTransactionService.getTransactionById(transactionId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transaction.fetched"),
                transaction));
    }

    /**
     * Retrieves transactions for a specific cash register.
     *
     * @param cashRegisterId cash register ID
     * @param page page number
     * @param size page size
     * @param sortBy sort field
     * @param sortDir sort direction
     * @return page of transaction DTOs
     */
    @GetMapping("/cash-transactions/register/{cashRegisterId}")
    @Operation(summary = "Get transactions by cash register",
            description = "Retrieves a paginated list of transactions for a cash register")
    public ResponseEntity<ApiResponse<Page<CashTransactionDto>>> getTransactionsByCashRegister(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<CashTransactionDto> transactions = cashTransactionService.getTransactionsByCashRegister(
                cashRegisterId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.register.fetched", cashRegisterId),
                transactions));
    }

    /**
     * Retrieves transactions for a specific invoice.
     *
     * @param invoiceId invoice ID
     * @return list of transaction DTOs
     */
    @GetMapping("/cash-transactions/invoice/{invoiceId}")
    @Operation(summary = "Get transactions by invoice",
            description = "Retrieves all cash transactions associated with an invoice")
    public ResponseEntity<ApiResponse<List<CashTransactionDto>>> getTransactionsByInvoice(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId) {

        List<CashTransactionDto> transactions = cashTransactionService.getTransactionsByInvoice(invoiceId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.invoice.fetched", invoiceId),
                transactions));
    }

    /**
     * Retrieves transactions within a date period.
     *
     * @param startDate period start
     * @param endDate period end
     * @param cashRegisterId optional cash register filter
     * @return list of transaction DTOs
     */
    @GetMapping("/cash-transactions/period")
    @Operation(summary = "Get transactions by period",
            description = "Retrieves cash transactions within a specified date period")
    public ResponseEntity<ApiResponse<List<CashTransactionDto>>> getTransactionsByPeriod(
            @Parameter(description = "Start date", example = "2026-01-01T00:00:00", required = true)
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date", example = "2026-12-31T23:59:59", required = true)
            @RequestParam LocalDateTime endDate,
            @Parameter(description = "Cash register ID (optional)", example = "1")
            @RequestParam(required = false) Long cashRegisterId) {

        List<CashTransactionDto> transactions = cashTransactionService.getTransactionsByPeriod(
                startDate, endDate, cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.period.fetched"),
                transactions));
    }

    /**
     * Cancels a cash transaction (creates a refund transaction).
     *
     * @param transactionId transaction ID
     * @param reason cancellation reason
     * @return refund transaction DTO
     */
    @PostMapping("/cash-transactions/{transactionId}/cancel")
    @Operation(summary = "Cancel cash transaction",
            description = "Cancels a cash transaction and creates a refund transaction")
    public ResponseEntity<ApiResponse<CashTransactionDto>> cancelCashTransaction(
            @Parameter(description = "Transaction ID", example = "1", required = true)
            @PathVariable Long transactionId,
            @Parameter(description = "Cancellation reason", example = "Wrong amount entered", required = true)
            @RequestParam String reason) {

        User currentUser = getCurrentUser();
        CashTransactionDto transaction = cashTransactionService.cancelTransaction(
                transactionId, reason, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transaction.cancelled"),
                transaction));
    }

    // =========================================================================
    // FINANCIAL STATISTICS
    // =========================================================================

    /**
     * Gets pending amounts split by order type (purchase vs sales).
     *
     * @return statistics DTO with pending amounts by type
     */
    @GetMapping("/statistics/pending-by-type")
    @Operation(summary = "Get pending amounts by order type",
            description = "Returns pending amounts for purchase orders and sales orders separately")
    public ResponseEntity<ApiResponse<InvoiceStatisticsDto>> getPendingAmountsByType() {
        InvoiceStatisticsDto statistics = invoiceService.getPendingAmountsByType();

        // Initialize localized fields
        String currencySymbol = "₽";   //TODO
        statistics.setLocalizedTotalPending(messageService.get("invoice.pending.total",
                formatBalance(statistics.getTotalPending()), currencySymbol));
        statistics.setLocalizedPurchasePending(messageService.get("invoice.pending.purchase",
                formatBalance(statistics.getPurchasePendingTotal()), currencySymbol));
        statistics.setLocalizedSalesPending(messageService.get("invoice.pending.sales",
                formatBalance(statistics.getSalesPendingTotal()), currencySymbol));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.statistics.pending.by.type"),
                statistics));
    }

    private String formatBalance(BigDecimal balance) {
        if (balance == null) return "0.00";
        return String.format("%,.2f", balance);
    }

    /**
     * Gets total paid amount for a period.
     *
     * @param startDate period start
     * @param endDate period end
     * @return total paid amount
     */
    @GetMapping("/statistics/paid-period")
    @Operation(summary = "Get total paid amount for period",
            description = "Calculates total amount paid for invoices within a date period")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPaidAmountForPeriod(
            @Parameter(description = "Start date", example = "2026-01-01T00:00:00", required = true)
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date", example = "2026-12-31T23:59:59", required = true)
            @RequestParam LocalDateTime endDate) {

        BigDecimal total = invoiceService.getTotalPaidAmountForPeriod(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.statistics.paid.period"),
                total));
    }

    /**
     * Gets current balance of a cash register.
     *
     * @param cashRegisterId cash register ID
     * @return current balance
     */
    @GetMapping("/statistics/cash-register/{cashRegisterId}/balance")
    @Operation(summary = "Get current cash register balance",
            description = "Retrieves the current calculated balance of a cash register")
    public ResponseEntity<ApiResponse<BigDecimal>> getCashRegisterBalance(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId) {

        BigDecimal balance = cashRegisterService.getCurrentBalance(cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.balance.fetched"),
                balance));
    }

    /**
     * Gets cash register summary for a period.
     *
     * @param cashRegisterId cash register ID
     * @param startDate period start
     * @param endDate period end
     * @return summary DTO with totals and statistics
     */
    @GetMapping("/statistics/cash-register/{cashRegisterId}/summary")
    @Operation(summary = "Get cash register summary for period",
            description = "Retrieves a financial summary for a cash register over a period")
    public ResponseEntity<ApiResponse<CashRegisterSummaryDto>> getCashRegisterSummary(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId,
            @Parameter(description = "Start date", example = "2026-01-01T00:00:00", required = true)
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "End date", example = "2026-12-31T23:59:59", required = true)
            @RequestParam LocalDateTime endDate) {

        CashRegisterSummaryDto summary = cashRegisterService.getSummary(cashRegisterId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.summary.fetched"),
                summary));
    }

    /**
     * Gets today's cash register summary.
     *
     * @param cashRegisterId cash register ID
     * @return summary DTO for today
     */
    @GetMapping("/statistics/cash-register/{cashRegisterId}/today")
    @Operation(summary = "Get today's cash register summary",
            description = "Retrieves a financial summary for a cash register for today")
    public ResponseEntity<ApiResponse<CashRegisterSummaryDto>> getTodayCashRegisterSummary(
            @Parameter(description = "Cash register ID", example = "1", required = true)
            @PathVariable Long cashRegisterId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);

        CashRegisterSummaryDto summary = cashRegisterService.getSummary(cashRegisterId, startOfDay, endOfDay);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.summary.today"),
                summary));
    }
}