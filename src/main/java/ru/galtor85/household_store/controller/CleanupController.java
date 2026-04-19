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
import ru.galtor85.household_store.dto.request.cleanup.SoftDeleteRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.cleanup.EntityCleanupService;
import ru.galtor85.household_store.service.i18n.MessageService;

/**
 * REST controller for managing soft deletion of entities.
 * <p>
 * Provides endpoints for administrators to soft delete:
 * <ul>
 *   <li>Sales orders</li>
 *   <li>Purchase orders</li>
 *   <li>Invoices</li>
 *   <li>Payment transactions</li>
 * </ul>
 * Also provides auto-cleanup of expired deleted entities.
 * </p>
 * <p>
 * All endpoints require ADMIN role for access.
 * </p>
 *
 * @author G@LTor85
 */
@Slf4j
@RestController
@RequestMapping("/app/admin/cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Cleanup Management", description = "Endpoints for soft deleting entities and auto-cleanup")
public class CleanupController extends BaseController {

    private final EntityCleanupService cleanupService;
    private final MessageService messageService;

    /**
     * Soft deletes a sales order by ID.
     * <p>
     * The order must be in CANCELLED or COMPLETED status.
     * The deletion is logical (soft delete) - the record remains in database
     * with 'deleted' flag set to true.
     * </p>
     *
     * @param orderId the ID of the sales order to delete
     * @param request contains the reason for deletion
     * @return success response with localized message
     */
    @DeleteMapping("/sales-orders/{orderId}")
    @Operation(summary = "Soft delete sales order",
            description = "Marks a sales order as deleted. Order must be CANCELLED or COMPLETED.")
    public ResponseEntity<ApiResponse<Void>> softDeleteSalesOrder(
            @Parameter(description = "Sales order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody SoftDeleteRequest request) {

        User currentUser = getCurrentUser();
        cleanupService.softDeleteSalesOrder(orderId, request.getReason(), currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cleanup.sales.order.deleted"), null));
    }

    /**
     * Soft deletes a purchase order by ID.
     * <p>
     * The order must be in CANCELLED status.
     * The deletion is logical (soft delete) - the record remains in database
     * with 'deleted' flag set to true.
     * </p>
     *
     * @param orderId the ID of the purchase order to delete
     * @param request contains the reason for deletion
     * @return success response with localized message
     */
    @DeleteMapping("/purchase-orders/{orderId}")
    @Operation(summary = "Soft delete purchase order",
            description = "Marks a purchase order as deleted. Order must be CANCELLED.")
    public ResponseEntity<ApiResponse<Void>> softDeletePurchaseOrder(
            @Parameter(description = "Purchase order ID", example = "1", required = true)
            @PathVariable Long orderId,
            @Valid @RequestBody SoftDeleteRequest request) {

        User currentUser = getCurrentUser();
        cleanupService.softDeletePurchaseOrder(orderId, request.getReason(), currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cleanup.purchase.order.deleted"), null));
    }

    /**
     * Soft deletes an invoice by ID.
     * <p>
     * The invoice must be in CANCELLED or REFUNDED status.
     * The deletion is logical (soft delete) - the record remains in database
     * with 'deleted' flag set to true.
     * </p>
     *
     * @param invoiceId the ID of the invoice to delete
     * @param request   contains the reason for deletion
     * @return success response with localized message
     */
    @DeleteMapping("/invoices/{invoiceId}")
    @Operation(summary = "Soft delete invoice",
            description = "Marks an invoice as deleted. Invoice must be CANCELLED or REFUNDED.")
    public ResponseEntity<ApiResponse<Void>> softDeleteInvoice(
            @Parameter(description = "Invoice ID", example = "1", required = true)
            @PathVariable Long invoiceId,
            @Valid @RequestBody SoftDeleteRequest request) {

        User currentUser = getCurrentUser();
        cleanupService.softDeleteInvoice(invoiceId, request.getReason(), currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cleanup.invoice.deleted"), null));
    }

    /**
     * Soft deletes a payment transaction by ID.
     * <p>
     * The transaction must be in FAILED or CANCELLED status and must be older than
     * the configured retention period (default: 3 months).
     * The deletion is logical (soft delete) - the record remains in database
     * with 'deleted' flag set to true.
     * </p>
     *
     * @param transactionId the ID of the payment transaction to delete
     * @param request       contains the reason for deletion
     * @return success response with localized message
     */
    @DeleteMapping("/payments/{transactionId}")
    @Operation(summary = "Soft delete payment transaction",
            description = "Marks a payment transaction as deleted. Transaction must be FAILED or CANCELLED and older than retention period.")
    public ResponseEntity<ApiResponse<Void>> softDeletePaymentTransaction(
            @Parameter(description = "Payment transaction ID", example = "1", required = true)
            @PathVariable Long transactionId,
            @Valid @RequestBody SoftDeleteRequest request) {

        User currentUser = getCurrentUser();
        cleanupService.softDeletePaymentTransaction(transactionId, request.getReason(), currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cleanup.payment.deleted"), null));
    }

    /**
     * Runs automatic cleanup of expired deleted entities.
     * <p>
     * Permanently removes entities that were soft deleted and have passed
     * the configured retention period (default: 90 days).
     * </p>
     *
     * @return success response with number of permanently deleted entities
     */
    @PostMapping("/auto-cleanup")
    @Operation(summary = "Run auto cleanup",
            description = "Permanently deletes soft-deleted entities that have passed the retention period.")
    public ResponseEntity<ApiResponse<Integer>> runAutoCleanup() {
        int deleted = cleanupService.cleanupExpiredDeletedEntities();
        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("cleanup.auto.completed", deleted), deleted));
    }
}