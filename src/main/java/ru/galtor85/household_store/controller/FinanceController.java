package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import ru.galtor85.household_store.dto.response.finance.CashRegisterDto;
import ru.galtor85.household_store.dto.response.finance.CashRegisterSummaryDto;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
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

@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Finance Operations", description = "Endpoints for managing invoices, cash registers and transactions")
public class FinanceController {

    private final InvoiceService invoiceService;
    private final CashRegisterService cashRegisterService;
    private final CashTransactionService cashTransactionService;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

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
    // УПРАВЛЕНИЕ СЧЕТАМИ (INVOICES)
    // =========================================================================

    @PostMapping("/invoices")
    @Operation(summary = "Create a new invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.createInvoice(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("invoice.created", invoice.getInvoiceNumber()),
                        invoice));
    }

    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(
            @PathVariable Long invoiceId) {

        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.fetched"),
                invoice));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Get all invoices with pagination")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "issueDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<InvoiceDto> invoices = invoiceService.getAllInvoices(page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.fetched"),
                invoices));
    }

    @GetMapping("/invoices/status/{status}")
    @Operation(summary = "Get invoices by status")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> getInvoicesByStatus(
            @PathVariable InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InvoiceDto> invoices = invoiceService.getInvoicesByStatus(status, page, size);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.status.fetched",
                        status.getLocalizedName(messageService)),
                invoices));
    }

    @GetMapping("/invoices/purchase-order/{orderId}")
    @Operation(summary = "Get invoices by purchase order")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesByPurchaseOrder(
            @PathVariable Long orderId) {

        List<InvoiceDto> invoices = invoiceService.getInvoicesByPurchaseOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.purchase.order.fetched", orderId),
                invoices));
    }

    @GetMapping("/invoices/sales-order/{orderId}")
    @Operation(summary = "Get invoices by sales order")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoicesBySalesOrder(
            @PathVariable Long orderId) {

        List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoices.by.sales.order.fetched", orderId),
                invoices));
    }

    @PutMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Mark invoice as paid")
    public ResponseEntity<ApiResponse<InvoiceDto>> markInvoiceAsPaid(
            @PathVariable Long invoiceId,
            @RequestParam Long cashRegisterId) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.markAsPaid(invoiceId, cashRegisterId, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.paid.success"),
                invoice));
    }

    @PutMapping("/invoices/{invoiceId}/partial-pay")
    @Operation(summary = "Partially pay invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> partiallyPayInvoice(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam Long cashRegisterId) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.markAsPartiallyPaid(invoiceId, amount, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.partially.paid.success", amount),
                invoice));
    }

    @PutMapping("/invoices/{invoiceId}/cancel")
    @Operation(summary = "Cancel invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancelInvoice(
            @PathVariable Long invoiceId,
            @RequestParam String reason) {

        User currentUser = getCurrentUser();
        InvoiceDto invoice = invoiceService.cancelInvoice(invoiceId, reason, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.cancelled.success"),
                invoice));
    }

    // =========================================================================
    // УПРАВЛЕНИЕ КАССАМИ (CASH REGISTERS)
    // =========================================================================

    @PostMapping("/cash-registers")
    @Operation(summary = "Create a new cash register")
    public ResponseEntity<ApiResponse<CashRegisterDto>> createCashRegister(
            @Valid @RequestBody CashRegisterCreateRequest request) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.createCashRegister(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cash.register.created"),
                        cashRegister));
    }

    @GetMapping("/cash-registers")
    @Operation(summary = "Get all cash registers")
    public ResponseEntity<ApiResponse<List<CashRegisterDto>>> getAllCashRegisters() {

        List<CashRegisterDto> cashRegisters = cashRegisterService.getAllCashRegisters();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.registers.fetched"),
                cashRegisters));
    }

    @GetMapping("/cash-registers/active")
    @Operation(summary = "Get all active cash registers")
    public ResponseEntity<ApiResponse<List<CashRegisterDto>>> getActiveCashRegisters() {

        List<CashRegisterDto> cashRegisters = cashRegisterService.getActiveCashRegisters();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.registers.active.fetched"),
                cashRegisters));
    }

    @GetMapping("/cash-registers/{cashRegisterId}")
    @Operation(summary = "Get cash register by ID")
    public ResponseEntity<ApiResponse<CashRegisterDto>> getCashRegisterById(
            @PathVariable Long cashRegisterId) {

        CashRegisterDto cashRegister = cashRegisterService.getCashRegisterById(cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.fetched"),
                cashRegister));
    }

    @PostMapping("/cash-registers/{cashRegisterId}/open")
    @Operation(summary = "Open cash register")
    public ResponseEntity<ApiResponse<CashRegisterDto>> openCashRegister(
            @PathVariable Long cashRegisterId,
            @RequestParam(required = false) BigDecimal openingBalance) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.openCashRegister(
                cashRegisterId, openingBalance, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.opened"),
                cashRegister));
    }

    @PostMapping("/cash-registers/{cashRegisterId}/close")
    @Operation(summary = "Close cash register")
    public ResponseEntity<ApiResponse<CashRegisterDto>> closeCashRegister(
            @PathVariable Long cashRegisterId,
            @RequestParam BigDecimal closingBalance) {

        User currentUser = getCurrentUser();
        CashRegisterDto cashRegister = cashRegisterService.closeCashRegister(
                cashRegisterId, closingBalance, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.closed"),
                cashRegister));
    }

    @PutMapping("/cash-registers/{cashRegisterId}")
    @Operation(summary = "Update cash register")
    public ResponseEntity<ApiResponse<CashRegisterDto>> updateCashRegister(
            @PathVariable Long cashRegisterId,
            @Valid @RequestBody CashRegisterUpdateRequest request) {

        CashRegisterDto cashRegister = cashRegisterService.updateCashRegister(cashRegisterId, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.updated"),
                cashRegister));
    }

    // =========================================================================
    // КАССОВЫЕ ОПЕРАЦИИ (CASH TRANSACTIONS)
    // =========================================================================

    @PostMapping("/cash-transactions")
    @Operation(summary = "Create a cash transaction")
    public ResponseEntity<ApiResponse<CashTransactionDto>> createCashTransaction(
            @Valid @RequestBody CashTransactionRequest request) {

        User currentUser = getCurrentUser();
        CashTransactionDto transaction = cashTransactionService.createTransaction(request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("cash.transaction.created"),
                        transaction));
    }

    @GetMapping("/cash-transactions/{transactionId}")
    @Operation(summary = "Get cash transaction by ID")
    public ResponseEntity<ApiResponse<CashTransactionDto>> getCashTransactionById(
            @PathVariable Long transactionId) {

        CashTransactionDto transaction = cashTransactionService.getTransactionById(transactionId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transaction.fetched"),
                transaction));
    }

    @GetMapping("/cash-transactions/register/{cashRegisterId}")
    @Operation(summary = "Get transactions by cash register")
    public ResponseEntity<ApiResponse<Page<CashTransactionDto>>> getTransactionsByCashRegister(
            @PathVariable Long cashRegisterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<CashTransactionDto> transactions = cashTransactionService.getTransactionsByCashRegister(
                cashRegisterId, page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.register.fetched", cashRegisterId),
                transactions));
    }

    @GetMapping("/cash-transactions/invoice/{invoiceId}")
    @Operation(summary = "Get transactions by invoice")
    public ResponseEntity<ApiResponse<List<CashTransactionDto>>> getTransactionsByInvoice(
            @PathVariable Long invoiceId) {

        List<CashTransactionDto> transactions = cashTransactionService.getTransactionsByInvoice(invoiceId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.invoice.fetched", invoiceId),
                transactions));
    }

    @GetMapping("/cash-transactions/period")
    @Operation(summary = "Get transactions by period")
    public ResponseEntity<ApiResponse<List<CashTransactionDto>>> getTransactionsByPeriod(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @RequestParam(required = false) Long cashRegisterId) {

        List<CashTransactionDto> transactions = cashTransactionService.getTransactionsByPeriod(
                startDate, endDate, cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transactions.by.period.fetched"),
                transactions));
    }

    @PostMapping("/cash-transactions/{transactionId}/cancel")
    @Operation(summary = "Cancel cash transaction")
    public ResponseEntity<ApiResponse<CashTransactionDto>> cancelCashTransaction(
            @PathVariable Long transactionId,
            @RequestParam String reason) {

        User currentUser = getCurrentUser();
        CashTransactionDto transaction = cashTransactionService.cancelTransaction(
                transactionId, reason, currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.transaction.cancelled"),
                transaction));
    }

    // =========================================================================
    // СТАТИСТИКА
    // =========================================================================

    @GetMapping("/statistics/pending-total")
    @Operation(summary = "Get total pending amount")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPendingAmount() {

        BigDecimal total = invoiceService.getTotalPendingAmount();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.statistics.pending.total"),
                total));
    }

    @GetMapping("/statistics/paid-period")
    @Operation(summary = "Get total paid amount for period")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPaidAmountForPeriod(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        BigDecimal total = invoiceService.getTotalPaidAmountForPeriod(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("invoice.statistics.paid.period"),
                total));
    }

    @GetMapping("/statistics/cash-register/{cashRegisterId}/balance")
    @Operation(summary = "Get current cash register balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getCashRegisterBalance(
            @PathVariable Long cashRegisterId) {

        BigDecimal balance = cashRegisterService.getCurrentBalance(cashRegisterId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.balance.fetched"),
                balance));
    }

    @GetMapping("/statistics/cash-register/{cashRegisterId}/summary")
    @Operation(summary = "Get cash register summary for period")
    public ResponseEntity<ApiResponse<CashRegisterSummaryDto>> getCashRegisterSummary(
            @PathVariable Long cashRegisterId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        CashRegisterSummaryDto summary = cashRegisterService.getSummary(cashRegisterId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cash.register.summary.fetched"),
                summary));
    }

    @GetMapping("/statistics/cash-register/{cashRegisterId}/today")
    @Operation(summary = "Get today's cash register summary")
    public ResponseEntity<ApiResponse<CashRegisterSummaryDto>> getTodayCashRegisterSummary(
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