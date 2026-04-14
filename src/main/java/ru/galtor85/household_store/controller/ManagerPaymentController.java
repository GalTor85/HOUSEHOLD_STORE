package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.payment.AssignPaymentMethodToUserTypeRequest;
import ru.galtor85.household_store.dto.request.payment.CreatePaymentMethodWithTypesRequest;
import ru.galtor85.household_store.dto.request.payment.ManagerCashPaymentRequest;
import ru.galtor85.household_store.dto.request.payment.PaymentProcessRequest;
import ru.galtor85.household_store.dto.response.payment.PaymentMethodWithUserTypesDto;
import ru.galtor85.household_store.dto.response.payment.PaymentTransactionDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.payment.PaymentMethodService;
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
    private final PaymentMethodService paymentMethodService;
    private final LogMessageService logMsg;


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
            @Valid @RequestBody PaymentProcessRequest request) {

        log.info(logMsg.get("payment.controller.manager.supplier.bank.request",
                request.getPurchaseOrderId(), request.getBankAccountId(), request.getAmount()));

        // Validate reference ID is purchase order ID
        if (request.getPurchaseOrderId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.purchase.order.id.required"));
        }
        if (request.getBankAccountId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.bank.account.id.required"));
        }

        PaymentTransactionDto transaction = paymentService.processPayment(request, getCurrentUserId());

        log.info(logMsg.get("payment.controller.manager.supplier.bank.success",
                request.getPurchaseOrderId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.supplier.paid"), transaction));
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
            @Valid @RequestBody PaymentProcessRequest request) {

        log.info(logMsg.get("payment.controller.manager.supplier.cash.request",
                request.getPurchaseOrderId(), request.getCashRegisterId(), request.getAmount()));

        // Transaction type must be EXPENSE for supplier payment
        if (request.getPurchaseOrderId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.purchase.order.id.required"));
        }
        if (request.getCashRegisterId() == null) {
            throw new IllegalArgumentException(messageService.get("payment.cash.register.id.required"));
        }

        // Invoice ID is required
        if (request.getInvoiceId() == null) {
            throw new IllegalArgumentException(
                    messageService.get("payment.invoice.id.required"));
        }

        PaymentTransactionDto transaction = paymentService.processPayment(request, getCurrentUserId());

        log.info(logMsg.get("payment.controller.manager.supplier.cash.success",
                request.getPurchaseOrderId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.supplier.paid"), transaction));
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
    @Operation(summary = "Receive cash payment from customer",
            description = "Manager receives cash payment from customer by order number or invoice number")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> receiveCashPayment(
            @Valid @RequestBody ManagerCashPaymentRequest request) {



        // Validate exactly one of orderNumber or invoiceNumber is provided
        if ((request.getOrderNumber() == null && request.getInvoiceNumber() == null) ||
                (request.getOrderNumber() != null && request.getInvoiceNumber() != null)) {
            throw new IllegalArgumentException(
                    messageService.get("payment.validation.order.or.invoice.required"));
        }

        String targetType;
        String targetNumber;
        if (request.getOrderNumber() != null) {
            targetType = "order";
            targetNumber = request.getOrderNumber();
        } else {
            targetType = "invoice";
            targetNumber = request.getInvoiceNumber();
        }

        log.info(logMsg.get("payment.controller.manager.customer.cash.request",
                targetType, targetNumber, request.getAmount(), request.getPaymentMethod()));

        PaymentTransactionDto transaction = paymentService.managerReceiveCashPayment(request, getCurrentUserId());

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
            @Valid @RequestBody PaymentProcessRequest request) {

        log.info(logMsg.get("payment.controller.manager.refund.request",
                request.getOriginalTransactionId(), request.getAmount(), request.getDescription()));

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

        PaymentTransactionDto transaction = paymentService.processPayment(request, getCurrentUserId());

        log.info(logMsg.get("payment.controller.manager.refund.success",
                request.getOriginalTransactionId(), transaction.getId()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.refund.processed"),
                transaction));
    }
// =========================================================================
// PAYMENT METHOD MANAGEMENT FOR USER TYPES (MANAGER ONLY)
// =========================================================================

    /**
     * Creates a payment method and assigns it to user types.
     *
     * @param request payment method creation request
     * @return created payment method DTO
     */
    @PostMapping("/methods/for-user-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create payment method for user types",
            description = "Creates a payment method and assigns it to specific user types")
    public ResponseEntity<ApiResponse<PaymentMethodWithUserTypesDto>> createPaymentMethodForUserTypes(
            @Valid @RequestBody CreatePaymentMethodWithTypesRequest request) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("payment.controller.manager.create.method.with.types.start",
                userId, request.getName(), request.getAvailableForUserTypes()));

        PaymentMethodWithUserTypesDto result = paymentMethodService.createPaymentMethodWithUserTypes(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("payment.method.created.with.types"),
                        result));
    }

    /**
     * Assigns existing payment method to user types.
     *
     * @param request assignment request
     * @return updated payment method DTO
     */
    @PostMapping("/methods/assign-to-user-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign payment method to user types",
            description = "Assigns an existing payment method to specific user types")
    public ResponseEntity<ApiResponse<PaymentMethodWithUserTypesDto>> assignPaymentMethodToUserTypes(
            @Valid @RequestBody AssignPaymentMethodToUserTypeRequest request) {

        Long userId = getCurrentUserId();
        log.info(logMsg.get("payment.controller.manager.assign.method.to.types.start",
                request.getPaymentMethodId(), request.getUserTypes()));

        paymentMethodService.assignPaymentMethodToUserTypes(
                request.getPaymentMethodId(),
                request.getUserTypes(),
                request.getSortOrder(),
                userId
        );

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.method.assigned.to.types"),
                null));
    }

    /**
     * Gets payment methods for a specific user type.
     *
     * @param userType the user type
     * @return list of payment methods
     */
    @GetMapping("/methods/by-user-type/{userType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get payment methods by user type",
            description = "Retrieves all payment methods available for a specific user type")
    public ResponseEntity<ApiResponse<List<PaymentMethodWithUserTypesDto>>> getPaymentMethodsByUserType(
            @Parameter(description = "User type", example = "RETAIL", required = true)
            @PathVariable UserType userType) {

        	log.debug(logMsg.get("payment.controller.manager.get.methods.by.type.start", userType));

        List<PaymentMethodWithUserTypesDto> result = paymentMethodService.getPaymentMethodsByUserType(userType);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.methods.by.type.fetched", userType),
                result));
    }

    // =========================================================================
// PAYMENT METHOD MANAGEMENT (ACTIVATE/DEACTIVATE/DELETE)
// =========================================================================

    /**
     * Activates a payment method.
     *
     * @param methodId payment method ID
     * @return activated payment method DTO
     */
    @PatchMapping("/methods/{methodId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Activate payment method",
            description = "Activates a payment method, making it available for users")
    public ResponseEntity<ApiResponse<PaymentMethodWithUserTypesDto>> activatePaymentMethod(
            @Parameter(description = "Payment method ID", example = "1", required = true)
            @PathVariable Long methodId) {

        log.info(logMsg.get("payment.controller.manager.activate.method.start", methodId));

        PaymentMethodWithUserTypesDto result = paymentMethodService.activatePaymentMethod(methodId);

        log.info(logMsg.get("payment.controller.manager.activate.method.success", methodId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.method.activated"),
                result));
    }

    /**
     * Deactivates a payment method.
     *
     * @param methodId payment method ID
     * @return deactivated payment method DTO
     */
    @PatchMapping("/methods/{methodId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Deactivate payment method",
            description = "Deactivates a payment method, hiding it from users")
    public ResponseEntity<ApiResponse<PaymentMethodWithUserTypesDto>> deactivatePaymentMethod(
            @Parameter(description = "Payment method ID", example = "1", required = true)
            @PathVariable Long methodId) {

        log.info(logMsg.get("payment.controller.manager.deactivate.method.start", methodId));

        PaymentMethodWithUserTypesDto result = paymentMethodService.deactivatePaymentMethod(methodId);

        log.info(logMsg.get("payment.controller.manager.deactivate.method.success", methodId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.method.deactivated"),
                result));
    }

    /**
     * Deletes a payment method permanently.
     *
     * @param methodId payment method ID
     * @return success response
     */
    @DeleteMapping("/methods/{methodId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete payment method",
            description = "Permanently deletes a payment method and all its user type assignments")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(
            @Parameter(description = "Payment method ID", example = "1", required = true)
            @PathVariable Long methodId) {

        log.info(logMsg.get("payment.controller.manager.delete.method.start", methodId));

        paymentMethodService.deletePaymentMethod(methodId);

        log.info(logMsg.get("payment.controller.manager.delete.method.success", methodId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.method.deleted"),
                null));
    }

    /**
     * Gets all payment methods (active and inactive) with user type assignments.
     *
     * @return list of all payment methods
     */
    @GetMapping("/methods/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all payment methods",
            description = "Retrieves all payment methods including inactive ones")
    public ResponseEntity<ApiResponse<List<PaymentMethodWithUserTypesDto>>> getAllPaymentMethods() {

        	log.debug(logMsg.get("payment.controller.manager.get.all.methods.start"));

        List<PaymentMethodWithUserTypesDto> result = paymentMethodService.getAllPaymentMethodsWithUserTypes();

        	log.debug(logMsg.get("payment.controller.manager.get.all.methods.success", result.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.methods.all.fetched"),
                result));
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

        log.info(logMsg.get("payment.controller.manager.history.supplier.request", purchaseOrderId));

        List<PaymentTransactionDto> transactions = paymentService.getSupplierPayments(purchaseOrderId);

        log.info(logMsg.get("payment.controller.manager.history.supplier.success",
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

        log.info(logMsg.get("payment.controller.manager.history.customer.request", salesOrderId));

        List<PaymentTransactionDto> transactions = paymentService.getCustomerPayments(salesOrderId);

        log.info(logMsg.get("payment.controller.manager.history.customer.success",
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

        log.info(logMsg.get("payment.controller.manager.transaction.request", transactionId));

        PaymentTransactionDto transaction = paymentService.getPaymentTransaction(transactionId);

        log.info(logMsg.get("payment.controller.manager.transaction.success", transactionId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("payment.transaction.fetched"),
                transaction));
    }
}