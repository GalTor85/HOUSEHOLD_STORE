package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.finance.BankAccountTransactionRequest;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.payment.PaymentService;

import java.math.BigDecimal;
import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_MANAGER_PAYMENTS;

/**
 * REST controller for manager payment operations.
 *
 * <p>This controller provides endpoints for managers and admins to:</p>
 * <ul>
 *   <li>Pay suppliers from company bank accounts</li>
 *   <li>Pay suppliers from cash registers</li>
 *   <li>Receive cash payments from customers at point of sale</li>
 *   <li>Process cash refunds to customers</li>
 *   <li>View cash register and bank account balances</li>
 *   <li>View payment transaction history</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN or MANAGER role for access.</p>
 */
@Slf4j
@RestController
@RequestMapping(CONTROL_MANAGER_PAYMENTS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Manager Payments", description = "Payment management endpoints for managers and admins")
public class ManagerPaymentController extends BaseController {

    private final PaymentService paymentService;
    private final MessageService messageService;

    // =========================================================================
    // SUPPLIER PAYMENTS
    // =========================================================================

    /**
     * Pay supplier from company bank account
     *
     * @param request bank account transaction request with account ID, purchase order ID and amount
     * @return payment transaction DTO with status and details
     */
    @PostMapping("/supplier/bank")
    @Operation(
            summary = "Pay supplier from bank account",
            description = "Manager pays supplier for purchase order from company bank account. " +
                    "The amount is withdrawn from the specified bank account."
    )
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> paySupplierFromBank(
            @Valid @RequestBody BankAccountTransactionRequest request) {

        log.info(messageService.get("payment.controller.manager.supplier.bank.request",
                request.getReferenceId(), request.getAccountId(), request.getAmount()));

        // Validate reference ID is purchase order ID
        if (request.getReferenceId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.purchase.order.id.required"));
        }

        PaymentTransactionDto transaction = paymentService.managerPaySupplierFromBank(
                request.getAccountId(),
                request.getReferenceId(),
                request.getAmount()
        );

        log.info(messageService.get("payment.controller.manager.supplier.bank.success",
                request.getReferenceId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.supplier.paid"),
                transaction));
    }

    /**
     * Pay supplier from cash register
     *
     * @param request cash transaction request with cash register ID, invoice ID and amount
     * @return payment transaction DTO with status and details
     */
    @PostMapping("/supplier/cash")
    @Operation(
            summary = "Pay supplier from cash register",
            description = "Manager pays supplier for purchase order from cash register. " +
                    "Creates an EXPENSE transaction in the cash register."
    )
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> paySupplierFromCash(
            @Valid @RequestBody CashTransactionRequest request) {

        log.info(messageService.get("payment.controller.manager.supplier.cash.request",
                request.getInvoiceId(), request.getCashRegisterId(), request.getAmount()));

        // Transaction type must be EXPENSE for supplier payment
        if (request.getTransactionType() != ru.galtor85.household_store.entity.finance.TransactionType.EXPENSE) {
            throw new IllegalArgumentException(
                    messageService.get("payment.supplier.payment.must.be.expense"));
        }

        // Invoice ID is required
        if (request.getInvoiceId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.invoice.id.required"));
        }

        PaymentTransactionDto transaction = paymentService.managerPaySupplierFromCash(
                request.getCashRegisterId(),
                request.getInvoiceId(),
                request.getAmount(),
                getCurrentUserId()
        );

        log.info(messageService.get("payment.controller.manager.supplier.cash.success",
                request.getInvoiceId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.supplier.paid"),
                transaction));
    }

    // =========================================================================
    // CUSTOMER CASH PAYMENTS
    // =========================================================================

    /**
     * Receive cash payment from customer at point of sale
     *
     * @param request cash transaction request with cash register ID, invoice ID, customer ID and amount
     * @return payment transaction DTO with status and details
     */
    @PostMapping("/customer/cash")
    @Operation(
            summary = "Receive cash payment from customer",
            description = "Manager receives cash payment from customer at point of sale. " +
                    "Creates an INCOME transaction in the cash register."
    )
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> receiveCashPayment(
            @Valid @RequestBody CashTransactionRequest request) {

        log.info(messageService.get("payment.controller.manager.customer.cash.request",
                request.getInvoiceId(), request.getCustomerId(), request.getAmount()));

        // Transaction type must be INCOME for customer payment
        if (request.getTransactionType() != ru.galtor85.household_store.entity.finance.TransactionType.INCOME) {
            throw new IllegalArgumentException(
                    messageService.get("payment.customer.payment.must.be.income"));
        }

        // Invoice ID is required
        if (request.getInvoiceId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.invoice.id.required"));
        }

        // Customer ID is required
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.customer.id.required"));
        }

        PaymentTransactionDto transaction = paymentService.managerReceiveCashPayment(
                request.getCashRegisterId(),
                request.getInvoiceId(),
                request.getAmount(),
                request.getCustomerId(),
                getCurrentUserId()
        );

        log.info(messageService.get("payment.controller.manager.customer.cash.success",
                request.getInvoiceId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.cash.received"),
                transaction));
    }

    // =========================================================================
    // CASH REFUNDS
    // =========================================================================

    /**
     * Process cash refund to customer
     *
     * @param request cash transaction request with cash register ID, original transaction ID, amount and reason
     * @return refund transaction DTO with status and details
     */
    @PostMapping("/cash-refund")
    @Operation(
            summary = "Process cash refund",
            description = "Manager processes cash refund to customer. " +
                    "Creates an EXPENSE transaction in the cash register and marks original payment as refunded."
    )
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> processCashRefund(
            @Valid @RequestBody CashTransactionRequest request) {

        log.info(messageService.get("payment.controller.manager.refund.request",
                request.getOriginalTransactionId(), request.getAmount(), request.getDescription()));

        // Transaction type must be REFUND
        if (request.getTransactionType() != ru.galtor85.household_store.entity.finance.TransactionType.REFUND) {
            throw new IllegalArgumentException(
                    messageService.get("payment.refund.must.be.refund.type"));
        }

        // Original transaction ID is required
        if (request.getOriginalTransactionId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.original.transaction.id.required"));
        }

        // Cash register ID is required
        if (request.getCashRegisterId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.cash.register.id.required"));
        }

        // Amount is required and must be positive
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    messageService.get("payment.amount.required"));
        }

        PaymentTransactionDto transaction = paymentService.managerProcessCashRefund(
                request.getCashRegisterId(),
                request.getOriginalTransactionId(),
                request.getAmount(),
                request.getDescription() != null ? request.getDescription() : "Customer refund",
                getCurrentUserId()
        );

        log.info(messageService.get("payment.controller.manager.refund.success",
                request.getOriginalTransactionId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.refund.processed"),
                transaction));
    }

    // =========================================================================
    // TRANSACTION HISTORY
    // =========================================================================

    /**
     * Get all payments made to supplier for a purchase order
     *
     * @param purchaseOrderId purchase order ID
     * @return list of payment transaction DTOs
     */
    @GetMapping("/transactions/supplier/{purchaseOrderId}")
    @Operation(
            summary = "Get supplier payment history",
            description = "Returns all payments made to supplier for the specified purchase order."
    )
    public ResponseEntity<ApiResponse<List<PaymentTransactionDto>>> getSupplierPayments(
            @Parameter(description = "Purchase order ID", example = "1", required = true
            )
            @PathVariable Long purchaseOrderId) {

        log.info(messageService.get("payment.controller.manager.history.supplier.request", purchaseOrderId));

        List<PaymentTransactionDto> transactions = paymentService.getSupplierPayments(purchaseOrderId);

        log.info(messageService.get("payment.controller.manager.history.supplier.success",
                purchaseOrderId, transactions.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.history.fetched"),
                transactions));
    }

    /**
     * Get all payments received from customer for a sales order
     *
     * @param salesOrderId sales order ID
     * @return list of payment transaction DTOs
     */
    @GetMapping("/transactions/customer/{salesOrderId}")
    @Operation(
            summary = "Get customer payment history",
            description = "Returns all payments received from customer for the specified sales order."
    )
    public ResponseEntity<ApiResponse<List<PaymentTransactionDto>>> getCustomerPayments(
            @Parameter(description = "Sales order ID", example = "1", required = true
            )
            @PathVariable Long salesOrderId) {

        log.info(messageService.get("payment.controller.manager.history.customer.request", salesOrderId));

        List<PaymentTransactionDto> transactions = paymentService.getCustomerPayments(salesOrderId);

        log.info(messageService.get("payment.controller.manager.history.customer.success",
                salesOrderId, transactions.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.history.fetched"),
                transactions));
    }

    /**
     * Get payment transaction by ID
     *
     * @param transactionId transaction ID
     * @return payment transaction DTO
     */
    @GetMapping("/transactions/{transactionId}")
    @Operation(
            summary = "Get payment transaction by ID",
            description = "Returns detailed information about a specific payment transaction."
    )
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> getPaymentTransaction(
            @Parameter(description = "Transaction ID", example = "1", required = true
            )
            @PathVariable Long transactionId) {

        log.info(messageService.get("payment.controller.manager.transaction.request", transactionId));

        PaymentTransactionDto transaction = paymentService.getPaymentTransaction(transactionId);

        log.info(messageService.get("payment.controller.manager.transaction.success", transactionId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.transaction.fetched"),
                transaction));
    }
}