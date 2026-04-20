package ru.galtor85.household_store.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.galtor85.household_store.advice.exception.auth.*;
import ru.galtor85.household_store.advice.exception.cart.CartEmptyException;
import ru.galtor85.household_store.advice.exception.cart.CartNotFoundException;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterClosedException;
import ru.galtor85.household_store.advice.exception.cash.CashRegisterNotFoundException;
import ru.galtor85.household_store.advice.exception.cash.InsufficientCashException;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.advice.exception.cell.*;
import ru.galtor85.household_store.advice.exception.file.*;
import ru.galtor85.household_store.advice.exception.finance.BankAccountNotFoundException;
import ru.galtor85.household_store.advice.exception.finance.InsufficientFundsException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyCancelledException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyPaidException;
import ru.galtor85.household_store.advice.exception.invoice.InvoiceAlreadyRefundedException;
import ru.galtor85.household_store.advice.exception.order.*;
import ru.galtor85.household_store.advice.exception.product.*;
import ru.galtor85.household_store.advice.exception.rollback.*;
import ru.galtor85.household_store.advice.exception.stock.*;
import ru.galtor85.household_store.advice.exception.supplier.*;
import ru.galtor85.household_store.advice.exception.system.DatabaseConnectionException;

import ru.galtor85.household_store.advice.exception.system.SystemInfoException;
import ru.galtor85.household_store.advice.exception.user.UserTypeAssignmentException;
import ru.galtor85.household_store.advice.exception.validation.InvalidDateRangeException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;

import ru.galtor85.household_store.advice.exception.validation.ValidationRequestException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.warehouse.WarehouseNotFoundException;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Global exception handler for REST controllers.
 * Provides centralized exception handling and localized error responses.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    // =========================================================================
    // SPRING MVC EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        String message = messageService.get("global-exception-handler.invalid.request");
        log.error("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        if (fieldErrors.isEmpty()) {
            String message = messageService.get("global-exception-handler.validation.general.error");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
        }
        FieldError fieldError = fieldErrors.getFirst();
        String message = fieldError.getDefaultMessage();
        if (message == null || message.startsWith("{")) {
            message = getValidationMessage(fieldError);
        }
        log.error("Validation error on field '{}': {}", fieldError.getField(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        String message = messageService.get("global-exception-handler.error.access.denied");
        log.error("AccessDeniedException");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    // =========================================================================
    // AUTHENTICATION & AUTHORIZATION
    // =========================================================================

    @ExceptionHandler({AuthenticationManagerException.class, CustomAuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationExceptions(RuntimeException e) {
        String message = messageService.get("global-exception-handler.error.authentication");
        log.error("Authentication exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials() {
        String message = messageService.get("auth.error.invalid.credentials");
        log.warn("InvalidCredentialsException");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAuthenticationError.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAuthenticationError(UserAuthenticationError e) {
        String message = messageService.get("global-exception-handler.auth.login.error", e.getMessage());
        log.error("UserAuthenticationError: {}", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    // =========================================================================
    // USER ACCOUNT EXCEPTIONS
    // =========================================================================

    @ExceptionHandler({UserNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound() {
        String message = messageService.get("global-exception-handler.error.user.not.found");
        log.error("UserNotFoundException");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SecurityUserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityUserNotFound(SecurityUserNotFoundException e) {
        String message = messageService.get("auth.error.security.user.not.found", e.getUserId());
        log.error("SecurityUserNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException e) {
        String message = messageService.get("global-exception-handler.auth.register.email.exists", e.getMessage());
        log.error("UserAlreadyExistsException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotActive() {
        String message = messageService.get("global-exception-handler.auth.login.user.inactive");
        log.error("UserNotActiveException");
        return ResponseEntity.status(HttpStatus.LOCKED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivated(AccountDeactivatedException e) {
        String message = messageService.get("auth.error.account.deactivated");
        log.warn("AccountDeactivatedException for user {}", e.getUserId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAccessException() {
        String message = messageService.get("global-exception-handler.error.user.access");
        log.error("UserAccessException");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    // =========================================================================
    // JWT TOKEN EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired() {
        String message = messageService.get("auth.error.token.expired");
        log.warn("TokenExpiredException");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenMalformedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenMalformed() {
        String message = messageService.get("auth.error.token.malformed");
        log.warn("TokenMalformedException");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenUnsupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenUnsupported() {
        String message = messageService.get("auth.error.token.unsupported");
        log.warn("TokenUnsupportedException");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenSecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenSecurity() {
        String message = messageService.get("auth.error.token.security");
        log.warn("TokenSecurityException");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RefreshTokenMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenMissing() {
        String message = messageService.get("auth.error.refresh.token.missing");
        log.warn("RefreshTokenMissingException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(IdentifierNotProvidedException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdentifierNotProvided() {
        String message = messageService.get("auth.error.identifier.not.provided");
        log.warn("IdentifierNotProvidedException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // USER TYPE EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(UserTypeAssignmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserTypeAssignmentException(UserTypeAssignmentException e) {
        log.error("UserTypeAssignmentException: {}", e.getMessage());
        Map<String, Object> details = new HashMap<>();
        details.put("userId", e.getUserId());
        details.put("userType", e.getUserType());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    // =========================================================================
    // VALIDATION EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(ValidationRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationRequestException(ValidationRequestException e) {
        String message = messageService.get("global-exception-handler.error.validation", e.getMessage());
        log.error("ValidationRequestException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDateRange(InvalidDateRangeException e) {
        String message = messageService.get("manager.order.error.date.range",
                e.getValidFrom(), e.getValidTo());
        log.error("InvalidDateRangeException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPrice(InvalidPriceException e) {
        String price = e.getInvalidPrice() != null ? e.getInvalidPrice().toString() : "null";
        String message = messageService.get("manager.price.error.invalid", price);
        log.error("InvalidPriceException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // PRODUCT EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException e) {
        String message = messageService.get("manager.product.error.not.found", e.getProductId());
        log.error("ProductNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductAlreadyExists(ProductAlreadyExistsException e) {
        String message = messageService.get("manager.product.error.exists", e.getField(), e.getValue());
        log.error("ProductAlreadyExistsException: field={}, value={}", e.getField(), e.getValue());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductInactive(ProductInactiveException e) {
        String message = messageService.get("product.error.inactive", e.getProductId());
        log.warn("ProductInactiveException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductVariantException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductVariantException(ProductVariantException e) {
        String message = messageService.get("manager.product.variant.error", e.getParentProductId());
        log.error("ProductVariantException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
// CATEGORY EXCEPTIONS
// =========================================================================

    // =========================================================================
    // PRODUCT MEDIA EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(ProductMediaException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductMediaException(ProductMediaException e) {
        log.error("ProductMediaException: {}", e.getMessage());
        Map<String, Object> details = new HashMap<>();
        details.put("productId", e.getProductId());
        details.put("fileName", e.getFileName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    @ExceptionHandler(ProductMediaUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductMediaUploadException(ProductMediaUploadException e) {
        log.error("ProductMediaUploadException: {}", e.getMessage());
        Map<String, Object> details = new HashMap<>();
        details.put("productId", e.getProductId());
        details.put("failedFiles", e.getFailedFiles());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    @ExceptionHandler(ProductMediaNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductMediaNotFoundException(
            ProductMediaNotFoundException e) {
        log.error("ProductMediaNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    }

    // =========================================================================
    // FILE STORAGE EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorageException(FileStorageException e) {
        String message = messageService.get("file.error.storage", e.getMessage());
        log.error("FileStorageException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileReadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileReadException(FileReadException e) {
        String message = messageService.get("file.error.read", e.getMessage());
        log.error("FileReadException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileDeleteException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileDeleteException(FileDeleteException e) {
        String message = messageService.get("file.error.delete", e.getFileName(), e.getProductId());
        log.error("FileDeleteException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSizeExceededException(FileSizeExceededException e) {
        String message = messageService.get("file.error.size.exceeded",
                e.getFileName(),
                formatFileSize(e.getMaxSize()),
                formatFileSize(e.getActualSize()));
        log.warn("FileSizeExceededException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFileTypeException(InvalidFileTypeException e) {
        String message = messageService.get("file.error.invalid.type",
                e.getFileName(),
                e.getContentType() != null ? e.getContentType() : messageService.get("file.type.unknown"),
                e.getAllowedTypes());
        log.warn("InvalidFileTypeException: {}", message);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.error(message));
    }

    // =========================================================================
    // ORDER EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException e) {
        String message = e.getOrderId() != null
                ? messageService.get("manager.order.error.not.found", e.getOrderId())
                : messageService.get("manager.orders.not.found");
        log.error("OrderNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SalesOrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSalesOrderNotFound(SalesOrderNotFoundException e) {
        String message = messageService.get("sales.order.not.found", e.getOrderId());
        log.error("SalesOrderNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderNotFound(PurchaseOrderNotFoundException e) {
        String message;
        if (e.getPurchaseOrderId() != null) {
            message = messageService.get("purchase.order.not.found.id", e.getPurchaseOrderId());
        } else if (e.getOrderNumber() != null) {
            message = messageService.get("purchase.order.not.found.number", e.getOrderNumber());
        } else {
            message = messageService.get("purchase.order.not.found");
        }
        log.error("PurchaseOrderNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderItemNotFound(OrderItemNotFoundException e) {
        String message = messageService.get("manager.order.error.item.not.found", e.getItemId());
        log.error("OrderItemNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderStatus(InvalidOrderStatusException e) {
        String message = messageService.get("manager.order.error.invalid.status", e.getInvalidStatus());
        log.error("InvalidOrderStatusException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

     @ExceptionHandler(OrderCancellationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderCancellationNotAllowed(
            OrderCancellationNotAllowedException e) {
        log.warn("OrderCancellationNotAllowedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    // =========================================================================
    // CART EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartNotFound(CartNotFoundException e) {
        String message = messageService.get("order.error.cart.not.found", e.getUserId());
        log.error("CartNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartEmpty() {
        String message = messageService.get("order.error.cart.empty");
        log.warn("CartEmptyException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // PURCHASE ORDER EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(CannotReceivePurchaseOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotReceivePurchaseOrder(
            CannotReceivePurchaseOrderException e) {
        String message = messageService.get("manager.purchase.error.cannot.receive", e.getCurrentStatus());
        log.error("CannotReceivePurchaseOrderException: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(PurchaseOrderCancellationException.class)
    public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderCancellationException(
            PurchaseOrderCancellationException e) {
        log.warn("Purchase order cancellation failed: {}", e.getMessage());
        Map<String, Object> details = new HashMap<>();
        if (e.getOrderId() != null) details.put("orderId", e.getOrderId());
        if (e.getCurrentStatus() != null) {
            details.put("currentStatus", e.getCurrentStatus().name());
            details.put("localizedStatus", messageService.get("order.status." + e.getCurrentStatus().name()));
        }
        if (e.getReason() != null) details.put("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    @ExceptionHandler(PurchaseOrderReverseException.class)
    public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderReverseException(
            PurchaseOrderReverseException e) {
        String message = messageService.get("purchase.order.cannot.reverse.default");
        log.warn("Purchase order reverse failed: {}", message);
        Map<String, Object> details = new HashMap<>();
        if (e.getOrderId() != null) details.put("orderId", e.getOrderId());
        if (e.getReason() != null) details.put("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message, details));
    }

    // =========================================================================
    // SUPPLIER EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierNotFound(SupplierNotFoundException e) {
        String message = e.getSupplierId() != null
                ? messageService.get("manager.supplier.error.not.found", e.getSupplierId())
                : messageService.get("manager.suppliers.error.not.found");
        log.error("SupplierNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierInactive(SupplierInactiveException e) {
        String message = messageService.get("manager.purchase.error.supplier.inactive", e.getCurrentStatus());
        log.error("SupplierInactiveException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierAlreadyExists(SupplierAlreadyExistsException e) {
        String message = messageService.get("manager.supplier.error." + e.getField() + ".exists", e.getValue());
        log.error("SupplierAlreadyExistsException: field={}, value={}", e.getField(), e.getValue());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierProductAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierProductAlreadyExists(
            SupplierProductAlreadyExistsException e) {
        String message = messageService.get("manager.supplier.error.product.already.added",
                e.getProductId(), e.getSupplierId());
        log.error("SupplierProductAlreadyExistsException: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    // =========================================================================
    // WAREHOUSE EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleWarehouseNotFound(WarehouseNotFoundException e) {
        String message;
        if (e.getWarehouseId() != null) {
            message = messageService.get("warehouse.error.not.found.id", e.getWarehouseId());
        } else if (e.getWarehouseCode() != null) {
            message = messageService.get("warehouse.error.not.found.code", e.getWarehouseCode());
        } else {
            message = messageService.get("warehouse.error.not.found");
        }
        log.error("WarehouseNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(WarehouseAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleWarehouseAlreadyExists(WarehouseAlreadyExistsException e) {
        String message = messageService.get("warehouse.error.already.exists");
        log.error("WarehouseAlreadyExistsException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    // =========================================================================
    // STORAGE CELL EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(CellNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellNotFound(CellNotFoundException e) {
        String message;
        if (e.getCellId() != null) {
            message = messageService.get("cell.error.not.found.id", e.getCellId());
        } else if (e.getCellCode() != null && e.getWarehouseId() != null) {
            message = messageService.get("cell.error.not.found.code", e.getCellCode(), e.getWarehouseId());
        } else {
            message = messageService.get("cells.error.not.found");
        }
        log.error("CellNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellAlreadyExists(CellAlreadyExistsException e) {
        String message = messageService.get("cell.error.already.exists",
                e.getCellCode(), e.getWarehouseId());
        log.error("CellAlreadyExistsException: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellAlreadyOccupiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellAlreadyOccupied(CellAlreadyOccupiedException e) {
        String message = messageService.get("cell.error.already.occupied", e.getCellId(), e.getCurrentProductId());
        log.error("CellAlreadyOccupiedException: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellInactive(CellInactiveException e) {
        String message = messageService.get("cell.error.inactive", e.getCellId());
        log.warn("CellInactiveException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(IncompatibleCellTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleIncompatibleCellType(IncompatibleCellTypeException e) {
        String message = messageService.get("cell.error.incompatible.type",
                e.getCellId(), e.getCellType(), e.getRequiredType());
        log.error("IncompatibleCellTypeException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellWeightLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellWeightLimitExceeded(
            CellWeightLimitExceededException e) {
        String message = messageService.get("cell.error.weight.limit.exceeded",
                e.getCellId(), e.getMaxWeight(), e.getRequestedWeight());
        log.error("CellWeightLimitExceededException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellVolumeLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellVolumeLimitExceeded(
            CellVolumeLimitExceededException e) {
        String message = messageService.get("cell.error.volume.limit.exceeded",
                e.getCellId(), e.getMaxVolume(), e.getRequestedVolume());
        log.error("CellVolumeLimitExceededException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoAvailableCellException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoAvailableCell(NoAvailableCellException e) {
        String message = messageService.get("cell.error.no.available",
                e.getWarehouseId(), e.getRequiredType());
        log.error("NoAvailableCellException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoSuitableCellException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuitableCell(NoSuitableCellException e) {
        String message = messageService.get("cell.error.no.suitable",
                e.getWarehouseId(), e.getRequiredType(), e.getProductId());
        log.error("NoSuitableCellException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductAlreadyInWarehouseException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductAlreadyInWarehouse(
            ProductAlreadyInWarehouseException e) {
        String message = messageService.get("cell.error.product.already.in.warehouse",
                e.getProductId(), e.getCellCode());
        log.warn("ProductAlreadyInWarehouseException: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    // =========================================================================
    // STOCK EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException e) {
        String message = messageService.get("manager.order.error.insufficient.stock",
                e.getProductName(), e.getAvailableStock());
        log.error("InsufficientStockException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(WriteOffInsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleWriteOffInsufficientStock(
            WriteOffInsufficientStockException e) {
        String message = messageService.get("manager.writeoff.error.insufficient.stock",
                e.getProductId(), e.getAvailableStock(), e.getRequestedQuantity());
        log.error("WriteOffInsufficientStockException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductStockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductStockNotFoundException(
            ProductStockNotFoundException e) {
        String message = messageService.get("stock.not.found");
        log.error("ProductStockNotFoundException: {}", message);
        Map<String, Object> details = new HashMap<>();
        if (e.getProductId() != null) details.put("productId", e.getProductId());
        if (e.getWarehouseId() != null) details.put("warehouseId", e.getWarehouseId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, details));
    }

    @ExceptionHandler(SameWarehouseTransferException.class)
    public ResponseEntity<ApiResponse<Void>> handleSameWarehouseTransfer(SameWarehouseTransferException e) {
        String message = messageService.get("stock.transfer.same.warehouse.error", e.getWarehouseId());
        log.warn("SameWarehouseTransferException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // CASH REGISTER EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(CashRegisterNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCashRegisterNotFound(CashRegisterNotFoundException e) {
        String message = e.getLocalizedMessage(messageService);
        log.error("CashRegisterNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(CashRegisterClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCashRegisterClosed(CashRegisterClosedException e) {
        String message = e.getLocalizedMessage(messageService);
        log.warn("CashRegisterClosedException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InsufficientCashException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientCash(InsufficientCashException e) {
        String message = e.getLocalizedMessage(messageService);
        log.warn("InsufficientCashException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
// BANK ACCOUNT EXCEPTIONS
// =========================================================================

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBankAccountNotFound(BankAccountNotFoundException e) {
        String message;
        if (e.getAccountId() != null) {
            message = messageService.get("bank.account.not.found.id", e.getAccountId());
        } else if (e.getAccountNumber() != null) {
            message = messageService.get("bank.account.not.found.number", e.getAccountNumber());
        } else {
            message = messageService.get("bank.account.not.found");
        }
        log.error("BankAccountNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientFunds(InsufficientFundsException e) {
        String message;
        if (e.getAccountId() != null && e.getCurrentBalance() != null && e.getRequestedAmount() != null) {
            message = messageService.get("bank.account.insufficient.funds",
                    e.getAccountId(),
                    formatMoney(e.getCurrentBalance()),
                    formatMoney(e.getRequestedAmount()));
        } else {
            message = messageService.get("bank.account.insufficient.funds.default");
        }
        log.warn("InsufficientFundsException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // INVOICE EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvoiceNotFoundException(InvoiceNotFoundException e) {
        String message = e.getLocalizedMessage(messageService);
        log.error("InvoiceNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvoiceAlreadyPaidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvoiceAlreadyPaid(InvoiceAlreadyPaidException e) {
        log.warn("InvoiceAlreadyPaidException: id={}, number={}", e.getInvoiceId(), e.getInvoiceNumber());
        String message = messageService.get("payment.error.invoice.already.paid");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvoiceAlreadyCancelledException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvoiceAlreadyCancelled(InvoiceAlreadyCancelledException e) {
        log.warn("InvoiceAlreadyCancelledException: id={}, number={}", e.getInvoiceId(), e.getInvoiceNumber());
        String message = messageService.get("invoice.already.cancelled");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvoiceAlreadyRefundedException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvoiceAlreadyRefunded(InvoiceAlreadyRefundedException e) {
        log.warn("InvoiceAlreadyRefundedException: id={}, number={}", e.getInvoiceId(), e.getInvoiceNumber());
        String message = messageService.get("invoice.already.refunded");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    // =========================================================================
    // ROLLBACK EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(RollbackAlreadyPendingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackAlreadyPending(RollbackAlreadyPendingException e) {
        String message = messageService.get("error.rollback.already.pending", e.getOrderId());
        log.warn("RollbackAlreadyPendingException for order {}", e.getOrderId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackApprovalNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackApprovalNotFound(RollbackApprovalNotFoundException e) {
        String message = messageService.get("error.rollback.approval.not.found", e.getApprovalId());
        log.error("RollbackApprovalNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackExecution(RollbackExecutionException e) {
        String message = messageService.get("error.rollback.execution.failed",
                e.getOrderId(), e.getErrorDetails());
        log.error("RollbackExecutionException for order {}: {}", e.getOrderId(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackFinalStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackFinalStatus(RollbackFinalStatusException e) {
        String localizedStatus = messageService.get("order.status." + e.getCurrentStatus().name());
        String message = messageService.get("error.rollback.final.status", localizedStatus);
        log.warn("RollbackFinalStatusException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackAlreadyProcessedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackAlreadyProcessed() {
        String message = messageService.get("error.rollback.already.processed");
        log.warn("RollbackAlreadyProcessedException");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackInvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackInvalidTransition(
            RollbackInvalidTransitionException e) {
        String current = messageService.get("order.status." + e.getCurrentStatus().name());
        String target = messageService.get("order.status." + e.getTargetStatus().name());
        String message = messageService.get("error.rollback.invalid.transition", current, target);
        log.warn("RollbackInvalidTransitionException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackNotAllowed(RollbackNotAllowedException e) {
        String message = e.getMessage();
        log.warn("RollbackNotAllowedException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // SYSTEM EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConnectionException(DatabaseConnectionException e) {
        String message = messageService.get("system.error.database.connection");
        log.error("DatabaseConnectionException: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(SystemInfoException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemInfoException(SystemInfoException e) {
        String message = messageService.get("system.error.info", e.getInfoType());
        log.error("SystemInfoException for {}: {}", e.getInfoType(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler({NoSuchMessageException.class, MissingResourceException.class})
    public ResponseEntity<ApiResponse<Void>> handleMessageSourceException(Exception e) {
        log.error("Message source error: {}", e.getMessage());
        String userMessage = messageService.get("global-exception-handler.error.configuration");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(userMessage));
    }

    // =========================================================================
    // JAVA STANDARD EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage() != null
                ? e.getMessage()
                : messageService.get("global-exception-handler.error.invalid.request");
        log.error("IllegalArgumentException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error("IllegalStateException: {}", e.getMessage());
        String message = messageService.getWithDefault("system.error.state",
                "System error: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedOperationException(UnsupportedOperationException e) {
        log.error("UnsupportedOperationException: {}", e.getMessage());
        String message = messageService.getWithDefault("system.error.unsupported",
                "Operation not supported");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleNumberFormatException(NumberFormatException e) {
        log.warn("NumberFormatException: {}", e.getMessage());
        String message = messageService.getWithDefault("validation.error.number.format",
                "Invalid number format");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    // =========================================================================
    // FALLBACK EXCEPTION HANDLER
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unhandled exception", e);
        String message = messageService.get("global-exception-handler.error.internal");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = messageService.get("error.data.integrity");
        log.error("DataIntegrityViolationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String getValidationMessage(FieldError fieldError) {
        String field = fieldError.getField();
        String code = fieldError.getCode();

        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return switch (field) {
                case "currentPassword" -> messageService.get(
                        "global-exception-handler.validation.password.current.required");
                case "newPassword" -> messageService.get(
                        "global-exception-handler.validation.password.new.empty");
                case "confirmPassword" -> messageService.get(
                        "global-exception-handler.validation.password.confirm.empty");
                default -> messageService.get("global-exception-handler.validation.field.required", field);
            };
        }

        if ("Size".equals(code)) {
            return switch (field) {
                case "currentPassword" -> messageService.get(
                        "global-exception-handler.validation.password.current.size", 6);
                case "newPassword" -> messageService.get(
                        "global-exception-handler.validation.password.new.size", 6);
                default -> messageService.get("global-exception-handler.validation.field.size", field);
            };
        }

        if ("Pattern".equals(code) && "newPassword".equals(field)) {
            return messageService.get("global-exception-handler.validation.password.new.invalid");
        }

        return messageService.get("global-exception-handler.validation.field.error", field);
    }

    private String formatFileSize(long size) {
        if (size < BYTES_IN_KB) {
            return size + " " + messageService.get("file.size.bytes");
        }
        if (size < BYTES_IN_MB) {
            return (size / BYTES_IN_KB) + " " + messageService.get("file.size.kb");
        }
        if (size < BYTES_IN_GB) {
            return (size / BYTES_IN_MB) + " " + messageService.get("file.size.mb");
        }
        return (size / BYTES_IN_GB) + " " + messageService.get("file.size.gb");
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }

}