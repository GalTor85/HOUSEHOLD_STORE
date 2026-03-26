package ru.galtor85.household_store.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.ApiResponse;
import ru.galtor85.household_store.service.MessageService;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    // =========================================================================
    // БЛОК 1: БАЗОВЫЕ ИСКЛЮЧЕНИЯ SPRING
    // =========================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        String message = messageService.get("global-exception-handler.invalid.request");
        log.error("HttpMessageNotReadableException: {} : {}", message, e.getLocalizedMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message + " :" + e.getLocalizedMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        String message = messageService.get("global-exception-handler.error.access.denied");
        log.error("AccessDeniedException: {}", message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        if (fieldErrors.isEmpty()) {
            String message = messageService.get("global-exception-handler.validation.general.error");
            String logMessage = messageService.get("global-exception-handler.log.validation.general.error", message);
            log.error(logMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(message));
        }

        FieldError fieldError = fieldErrors.get(0);
        String message = fieldError.getDefaultMessage();

        if (message == null || message.startsWith("{")) {
            message = getValidationMessage(fieldError);
        }

        String errorLog = messageService.get("global-exception-handler.log.validation.field.error",
                fieldError.getField(), message);
        log.error(errorLog);

        if (fieldError.getRejectedValue() == null) {
            String debugLog = messageService.get("global-exception-handler.log.validation.field.missing",
                    fieldError.getField());
            log.debug(debugLog);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 2: АУТЕНТИФИКАЦИЯ И ПОЛЬЗОВАТЕЛИ
    // =========================================================================

    @ExceptionHandler({AuthenticationManagerException.class, CustomAuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationExceptions(
            RuntimeException e) {
        String message = messageService.get("global-exception-handler.error.authentication");
        String logMessage = messageService.get("global-exception-handler.log.authentication.error", e.getMessage());
        log.error(logMessage);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
            UserNotFoundException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.not.found");
        log.error("UserNotFoundException: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAccessException() {
        String message = messageService.get("global-exception-handler.error.user.access");
        log.error("UserAccessException: {}", message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAuthenticationError.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAuthenticationError(
            UserAuthenticationError e) {
        String message = messageService.get("global-exception-handler.auth.login.error", e.getMessage());
        log.error("UserAuthenticationError: {}", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
            UserAlreadyExistsException e) {
        String message = messageService.get("global-exception-handler.auth.register.email.exists", e.getMessage());
        log.error("UserAlreadyExistsException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotActive() {
        String message = messageService.get("global-exception-handler.auth.login.user.inactive");
        log.error("UserNotActiveException: {}", message);
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound() {

        String message = messageService.get("global-exception-handler.error.user.not.found");
        log.error("UsernameNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 3: ТИПЫ ПОЛЬЗОВАТЕЛЕЙ (USER TYPE)
    // =========================================================================

    @ExceptionHandler(UserTypeAssignmentException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserTypeAssignmentException(
            UserTypeAssignmentException e) {

        log.error("UserTypeAssignmentException: {}", e.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("userId", e.getUserId());
        details.put("userType", e.getUserType());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    @ExceptionHandler(UserTypeAssignmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserTypeAssignmentNotFound(
            UserTypeAssignmentNotFoundException e) {

        String message = messageService.get("user-type.error.not.found", e.getUserId());
        log.error("UserTypeAssignmentNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 4: ВАЛИДАЦИЯ
    // =========================================================================

    @ExceptionHandler(ValidationRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationRequestException(
            ValidationRequestException e) {

        String message = messageService.get("global-exception-handler.error.validation", e.getMessage());
        String errorLog = messageService.get("global-exception-handler.log.validation.request.exception", message);
        log.error(errorLog);

        if (e.getPrincipal() != null) {
            String warnLog = messageService.get("global-exception-handler.log.validation.request.principal",
                    e.getPrincipal());
            log.warn(warnLog);
        }

        String responseMessage;
        if (e.getPrincipal() != null) {
            responseMessage = messageService.get("global-exception-handler.error.validation.with.principal",
                    message, e.getPrincipal());
        } else {
            responseMessage = message;
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(responseMessage));
    }

    // =========================================================================
    // БЛОК 5: ПРОДУКТЫ
    // =========================================================================

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductAlreadyExists(
            ProductAlreadyExistsException e) {

        String message = messageService.get(
                "manager.product.error.exists",
                e.getField(),
                e.getValue()
        );

        log.error("ProductAlreadyExistsException: {} - {}: {}",
                message, e.getField(), e.getValue());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(
            ProductNotFoundException e) {

        String message = messageService.get(
                "manager.product.error.not.found",
                e.getProductId()
        );

        log.error("ProductNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidStockOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStockOperation(
            InvalidStockOperationException e) {

        String message = messageService.get(
                "manager.stock.error.negative",
                e.getCurrentStock()
        );

        log.error("InvalidStockOperationException: Current stock: {}, Requested change: {} - {}",
                e.getCurrentStock(), e.getRequestedChange(), message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPrice(
            InvalidPriceException e) {

        String message = messageService.get(
                "manager.price.error.invalid",
                e.getInvalidPrice() != null ? e.getInvalidPrice().toString() : "null"
        );

        log.error("InvalidPriceException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ProductVariantException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductVariantException(
            ProductVariantException e) {

        String message = messageService.get(
                "manager.product.variant.error",
                e.getParentProductId()
        );

        log.error("ProductVariantException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(BulkOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBulkOperationException(
            BulkOperationException e) {

        String message = messageService.get(
                "manager.bulk.operation.error",
                e.getSuccessfulCount(),
                e.getProductIds().size()
        );

        log.error("BulkOperationException: {}", message);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 6: МЕДИА ПРОДУКТОВ
    // =========================================================================

    @ExceptionHandler(ProductMediaException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductMediaException(
            ProductMediaException e) {

        log.error("ProductMediaException: {}", e.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("productId", e.getProductId());
        details.put("fileName", e.getFileName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), details));
    }

    @ExceptionHandler(ProductMediaUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductMediaUploadException(
            ProductMediaUploadException e) {

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

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    // =========================================================================
    // БЛОК 7: ФАЙЛОВОЕ ХРАНИЛИЩЕ
    // =========================================================================

    @ExceptionHandler(FileDeleteException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileDeleteException(
            FileDeleteException e) {

        String message = messageService.get(
                "file.error.delete",
                e.getFileName(),
                e.getProductId()
        );

        log.error("FileDeleteException: {}", message, e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileReadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileReadException(
            FileReadException e) {

        String message;
        if (e.getFileName() != null && e.getProductId() != null) {
            message = messageService.get(
                    "file.error.read.with.details",
                    e.getFileName(),
                    e.getProductId()
            );
        } else {
            message = messageService.get("file.error.read", e.getMessage());
        }

        log.error("FileReadException: {}", message, e);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSizeExceededException(
            FileSizeExceededException e) {

        String message = messageService.get(
                "file.error.size.exceeded",
                e.getFileName(),
                formatFileSize(e.getMaxSize()),
                formatFileSize(e.getActualSize())
        );

        log.warn("FileSizeExceededException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorageException(
            FileStorageException e) {

        String message;
        if (e.getFileName() != null && e.getProductId() != null) {
            message = messageService.get(
                    "file.error.storage.with.details",
                    e.getFileName(),
                    e.getProductId(),
                    e.getMessage()
            );
        } else if (e.getProductId() != null) {
            message = messageService.get(
                    "file.error.storage.product",
                    e.getProductId(),
                    e.getMessage()
            );
        } else {
            message = messageService.get("file.error.storage", e.getMessage());
        }

        log.error("FileStorageException: {}", message, e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFileTypeException(
            InvalidFileTypeException e) {

        String message = messageService.get(
                "file.error.invalid.type",
                e.getFileName(),
                e.getContentType() != null ? e.getContentType() : "unknown",
                e.getAllowedTypes()
        );

        log.warn("InvalidFileTypeException: {}", message);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 8: ЗАКАЗЫ
    // =========================================================================

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(
            OrderNotFoundException e) {

        if (e.getOrderId() == null) {
            String message = messageService.get("manager.orders.not.found");
            log.error("OrderNotFoundException: {}", message);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(message));

        } else {

            String message = messageService.get("manager.order.error.not.found", e.getOrderId());
            log.error("OrderNotFoundException: {}", message);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(message));
        }
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderStatus(
            InvalidOrderStatusException e) {

        String message = messageService.get("manager.order.error.invalid.status", e.getInvalidStatus());
        log.error("InvalidOrderStatusException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatusTransition(
            InvalidStatusTransitionException e, Locale locale) {

        String message = messageService.get(
                "manager.order.error.invalid.status.transition",
                e.getCurrentStatus(),
                e.getNewStatus()
        );
        log.error("InvalidStatusTransitionException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderItemNotFound(
            OrderItemNotFoundException e) {

        String message = messageService.get("manager.order.error.item.not.found", e.getItemId());
        log.error("OrderItemNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(OrderModificationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderModificationNotAllowed(
            OrderModificationNotAllowedException e) {

        String message = messageService.get(
                "manager.order.error.cannot.modify",
                e.getCurrentStatus()
        );
        log.error("OrderModificationNotAllowedException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidOrderTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderType(
            InvalidOrderTypeException e) {

        String message = messageService.get(
                "manager.order.error.not.customer.order",
                e.getOrderId()
        );
        log.error("InvalidOrderTypeException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDateRange(
            InvalidDateRangeException e) {

        String message = messageService.get(
                "manager.order.error.date.range",
                e.getValidFrom(),
                e.getValidTo()
        );
        log.error("InvalidDateRangeException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(OrderItemAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderItemAlreadyExists(
            OrderItemAlreadyExistsException e) {

        String message = messageService.get("manager.order.error.item.exists", e.getProductId());
        log.error("OrderItemAlreadyExistsException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(
            InsufficientStockException e) {

        String message = messageService.get(
                "manager.order.error.insufficient.stock",
                e.getProductName(),
                e.getAvailableStock()
        );
        log.error("InsufficientStockException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidQuantity(
            InvalidQuantityException e) {

        String message = messageService.get(
                "manager.order.error.invalid.quantity",
                e.getInvalidQuantity() != null ? e.getInvalidQuantity().toString() : "null"
        );
        log.error("InvalidQuantityException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartNotFound(
            CartNotFoundException e) {

        String message = messageService.get("order.error.cart.not.found", e.getUserId());
        log.error("CartNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartEmpty() {

        String message = messageService.get("order.error.cart.empty");
        log.warn("CartEmptyException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderCreation(
            OrderCreationException e) {

        log.error("OrderCreationException for salesOrder {}: {}", e.getOrderNumber(), e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
    }

    // =========================================================================
    // БЛОК 9: ПОСТАВЩИКИ
    // =========================================================================

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierNotFound(
            SupplierNotFoundException e) {
        if (e.getSupplierId() != null) {

            String message = messageService.get("manager.supplier.error.not.found", e.getSupplierId());
            log.error("SupplierNotFoundException: {}", message);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(message));
        } else {
            String message = messageService.get("manager.suppliers.error.not.found");
            log.error("SupplierNotFoundException: {}", message);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(message));
        }
    }

    @ExceptionHandler(SupplierInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierInactive(
            SupplierInactiveException e) {

        String message = messageService.get("manager.purchase.error.supplier.inactive", e.getCurrentStatus());
        log.error("SupplierInactiveException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierAlreadyExists(
            SupplierAlreadyExistsException e) {

        String message = messageService.get("manager.supplier.error." + e.getField() + ".exists", e.getValue());
        log.error("SupplierAlreadyExistsException: {} - {}: {}", e.getMessage(), e.getField(), e.getValue());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierProductAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierProductAlreadyExists(
            SupplierProductAlreadyExistsException e) {

        String message = messageService.get(
                "manager.supplier.error.product.already.added",
                e.getProductId(),
                e.getSupplierId()
        );
        log.error("SupplierProductAlreadyExistsException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SupplierProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSupplierProductNotFound(
            SupplierProductNotFoundException e) {

        String message = messageService.get("manager.supplier.product.error.not.found", e.getSupplierProductId());
        log.error("SupplierProductNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 10: ЗАКУПКИ
    // =========================================================================

    @ExceptionHandler(ProductNotFromSupplierException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFromSupplier(
            ProductNotFromSupplierException e) {

        String message = messageService.get(
                "manager.purchase.error.product.not.from.supplier",
                e.getProductId(),
                e.getSupplierId()
        );
        log.error("ProductNotFromSupplierException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(PurchaseOrderDetailsNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePurchaseOrderDetailsNotFound(
            PurchaseOrderDetailsNotFoundException e) {

        String message = messageService.get("manager.purchase.error.purchase.details.not.found", e.getOrderId());
        log.error("PurchaseOrderDetailsNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CannotReceivePurchaseOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotReceivePurchaseOrder(
            CannotReceivePurchaseOrderException e) {

        String message = messageService.get("manager.purchase.error.cannot.receive", e.getCurrentStatus());
        log.error("CannotReceivePurchaseOrderException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 11: СПИСАНИЕ ТОВАРОВ
    // =========================================================================

    @ExceptionHandler(WriteOffInsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleWriteOffInsufficientStock(
            WriteOffInsufficientStockException e) {

        String message = messageService.get(
                "manager.writeoff.error.insufficient.stock",
                e.getProductId(),
                e.getAvailableStock(),
                e.getRequestedQuantity()
        );
        log.error("WriteOffInsufficientStockException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 12: ТОКЕНЫ И АУТЕНТИФИКАЦИЯ
    // =========================================================================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials() {

        String message = messageService.get("auth.error.invalid.credentials");
        log.warn("InvalidCredentialsException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivated(
            AccountDeactivatedException e) {

        String message = messageService.get("auth.error.account.deactivated");
        log.warn("AccountDeactivatedException for user {}: {}", e.getUserId(), message);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired() {

        String message = messageService.get("auth.error.token.expired");
        log.warn("TokenExpiredException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenMalformedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenMalformed(
            TokenMalformedException e, Locale locale) {

        String message = messageService.get("auth.error.token.malformed");
        log.warn("TokenMalformedException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenUnsupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenUnsupported() {

        String message = messageService.get("auth.error.token.unsupported");
        log.warn("TokenUnsupportedException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenSecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenSecurity() {

        String message = messageService.get("auth.error.token.security");
        log.warn("TokenSecurityException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidTokenFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTokenFormat() {

        String message = messageService.get("auth.error.token.invalid.format");
        log.warn("InvalidTokenFormatException: {}", message);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RefreshTokenMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenMissing() {

        String message = messageService.get("auth.error.refresh.token.missing");
        log.warn("RefreshTokenMissingException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(IdentifierNotProvidedException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdentifierNotProvided() {

        String message = messageService.get("auth.error.identifier.not.provided");
        log.warn("IdentifierNotProvidedException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SecurityUserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityUserNotFound(
            SecurityUserNotFoundException e) {

        String message = messageService.get("auth.error.security.user.not.found", e.getUserId());
        log.error("SecurityUserNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 13: СИСТЕМНЫЕ ИСКЛЮЧЕНИЯ
    // =========================================================================

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseConnectionException(
            DatabaseConnectionException e) {

        String message = messageService.get("system.error.database.connection");
        log.error("DatabaseConnectionException: {}", message, e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SystemInfoException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemInfoException(
            SystemInfoException e) {

        String message = messageService.get("system.error.info", e.getInfoType());
        log.error("SystemInfoException for {}: {}", e.getInfoType(), e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    // ========== WAREHOUSE EXCEPTIONS ==========

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleWarehouseNotFound(
            WarehouseNotFoundException e) {

        String message;
        if (e.getWarehouseId() != null) {
            message = messageService.get("warehouse.error.not.found.id", e.getWarehouseId());
        } else if (e.getWarehouseCode() != null) {
            message = messageService.get("warehouse.error.not.found.code", e.getWarehouseCode());
        } else {
            message = messageService.get("warehouse.error.not.found");
        }

        log.error("WarehouseNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(WarehouseAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleWarehouseAlreadyExists(
            WarehouseAlreadyExistsException e) {

        String message = messageService.get(
                "warehouse.error.already.exists",
                e.getField(),
                e.getValue()
        );

        log.error("WarehouseAlreadyExistsException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

// ========== CELL EXCEPTIONS ==========

    @ExceptionHandler(CellNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellNotFound(
            CellNotFoundException e) {

        String message;
        if (e.getCellId() != null) {
            message = messageService.get("cell.error.not.found.id", e.getCellId());
        } else if (e.getCellCode() != null && e.getWarehouseId() != null) {
            message = messageService.get("cell.error.not.found.code",
                    e.getCellCode(), e.getWarehouseId());
        } else {
            message = messageService.get("cells.error.not.found");
        }

        log.error("CellNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellAlreadyExists(
            CellAlreadyExistsException e) {

        String message = messageService.get(
                "cell.error.already.exists",
                e.getCellCode(),
                e.getWarehouseId()
        );

        log.error("CellAlreadyExistsException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellAlreadyOccupiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellAlreadyOccupied(
            CellAlreadyOccupiedException e) {

        String message;
        if (e.getCellId() != null) {
            message = messageService.get(
                    "cell.error.already.occupied.id",
                    e.getCellId(),
                    e.getCurrentProductId()
            );
        } else {
            message = messageService.get(
                    "cell.error.already.occupied.code",
                    e.getCellCode(),
                    e.getCurrentProductId()
            );
        }

        log.error("CellAlreadyOccupiedException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellAlreadyEmptyException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellAlreadyEmpty(
            CellAlreadyEmptyException e) {

        String message;
        if (e.getCellId() != null) {
            message = messageService.get("cell.error.already.empty.id", e.getCellId());
        } else {
            message = messageService.get("cell.error.already.empty.code", e.getCellCode());
        }

        log.warn("CellAlreadyEmptyException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InsufficientCellCapacityException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientCellCapacity(
            InsufficientCellCapacityException e) {

        String message;
        if (e.getCellId() != null) {
            message = messageService.get(
                    "cell.error.insufficient.capacity.id",
                    e.getCellId(),
                    e.getAvailableQuantity(),
                    e.getRequestedQuantity()
            );
        } else {
            message = messageService.get(
                    "cell.error.insufficient.capacity.code",
                    e.getCellCode(),
                    e.getAvailableQuantity(),
                    e.getRequestedQuantity()
            );
        }

        log.error("InsufficientCellCapacityException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(IncompatibleCellTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleIncompatibleCellType(
            IncompatibleCellTypeException e) {

        String message;
        if (e.getCellId() != null) {
            message = messageService.get(
                    "cell.error.incompatible.type.id",
                    e.getCellId(),
                    e.getCellType(),
                    e.getRequiredType()
            );
        } else {
            message = messageService.get(
                    "cell.error.incompatible.type.code",
                    e.getCellCode(),
                    e.getCellType(),
                    e.getRequiredType()
            );
        }

        log.error("IncompatibleCellTypeException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellWeightLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellWeightLimitExceeded(
            CellWeightLimitExceededException e) {

        String message = messageService.get(
                "cell.error.weight.limit.exceeded",
                e.getCellId(),
                e.getMaxWeight(),
                e.getRequestedWeight()
        );

        log.error("CellWeightLimitExceededException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(CellVolumeLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellVolumeLimitExceeded(
            CellVolumeLimitExceededException e) {

        String message = messageService.get(
                "cell.error.volume.limit.exceeded",
                e.getCellId(),
                e.getMaxVolume(),
                e.getRequestedVolume()
        );

        log.error("CellVolumeLimitExceededException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

// ========== STOCK MOVEMENT EXCEPTIONS ==========

    @ExceptionHandler(StockMovementNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockMovementNotFound(
            StockMovementNotFoundException e) {

        String message = messageService.get(
                "movement.error.not.found",
                e.getMovementId()
        );

        log.error("StockMovementNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvalidStockMovementException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStockMovement(
            InvalidStockMovementException e) {

        String message = messageService.get(
                "movement.error.invalid",
                e.getReason(),
                e.getProductId()
        );

        log.error("InvalidStockMovementException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoAvailableCellException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoAvailableCell(
            NoAvailableCellException e) {

        String message = messageService.get("cell.error.no.available",
                e.getWarehouseId(), e.getRequiredType());
        log.error("NoAvailableCellException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(NoSuitableCellException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuitableCell(
            NoSuitableCellException e) {

        String message = messageService.get("cell.error.no.suitable",
                e.getWarehouseId(), e.getRequiredType(), e.getProductId());
        log.error("NoSuitableCellException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 14: СООБЩЕНИЯ И РЕСУРСЫ
    // =========================================================================

    @ExceptionHandler({NoSuchMessageException.class, MissingResourceException.class})
    public ResponseEntity<ApiResponse<Void>> handleMessageSourceException(
            Exception e) {
        String logMessage = messageService.get("global-exception-handler.log.message.source.error", e.getMessage());
        log.error(logMessage);
        String userMessage = messageService.get("global-exception-handler.error.configuration");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(userMessage));
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotFoundException(
            MessageNotFoundException e) {

        log.error("MessageNotFoundException: {}", e.getMessage());

        String message = messageService.getWithDefault(
                "global-exception-handler.error.configuration.missing.key",
                "Configuration error: missing message key"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    // ========== ROLLBACK EXCEPTIONS ==========

    @ExceptionHandler(RollbackAlreadyPendingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackAlreadyPending(
            RollbackAlreadyPendingException e) {

        String message = messageService.get(
                "error.rollback.already.pending",
                e.getOrderId()
        );

        log.warn("RollbackAlreadyPendingException for salesOrder {}: {}",
                e.getOrderId(), message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackApprovalNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackApprovalNotFound(
            RollbackApprovalNotFoundException e) {

        String message = messageService.get(
                "error.rollback.approval.not.found",
                e.getApprovalId()
        );

        log.error("RollbackApprovalNotFoundException: {}", message);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackExecution(
            RollbackExecutionException e) {

        String message = messageService.get(
                "error.rollback.execution.failed",
                e.getOrderId(),
                e.getErrorDetails()
        );

        log.error("RollbackExecutionException for salesOrder {}: {}",
                e.getOrderId(), e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackFinalStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackFinalStatus(
            RollbackFinalStatusException e) {

        String localizedStatus = messageService.get(
                "salesOrder.status." + e.getCurrentStatus().name()
        );

        String message = messageService.get(
                "error.rollback.final.status",
                localizedStatus
        );

        log.warn("RollbackFinalStatusException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackAlreadyProcessedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackAlreadyProcessed() {

        String message = messageService.get("error.rollback.already.processed");

        log.warn("RollbackAlreadyProcessedException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackInvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackInvalidTransition(
            RollbackInvalidTransitionException e) {

        String localizedCurrent = messageService.get(
                "salesOrder.status." + e.getCurrentStatus().name()
        );
        String localizedTarget = messageService.get(
                "salesOrder.status." + e.getTargetStatus().name()
        );

        String message = messageService.get(
                "error.rollback.invalid.transition",
                localizedCurrent,
                localizedTarget
        );

        log.warn("RollbackInvalidTransitionException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackInvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackInvalidState(
            RollbackInvalidStateException e) {

        String message = messageService.get(
                "error.rollback.invalid.state",
                e.getOrderId(),
                messageService.get("salesOrder.status." + e.getCurrentStatus().name()),
                e.getDetails()
        );

        log.warn("RollbackInvalidStateException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackTimeout(
            RollbackTimeoutException e) {

        String message = messageService.get(
                "error.rollback.timeout",
                e.getOrderId(),
                e.getDeliveredAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        log.warn("RollbackTimeoutException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(RollbackNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRollbackNotAllowed(
            RollbackNotAllowedException e) {

        String message;
        if (e.getCurrentStatus() != null) {
            String localizedStatus = messageService.get("order.status." + e.getCurrentStatus().name());
            if (e.getOrderId() != null) {
                message = messageService.get("rollback.not.allowed.for.order",
                        e.getOrderId(), localizedStatus);
            } else {
                message = messageService.get("rollback.not.allowed.for.status", localizedStatus);
            }
        } else {
            message = e.getMessage();
        }

        log.warn("RollbackNotAllowedException: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // БЛОК 15: ОБЩИЙ ОБРАБОТЧИК
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception e) {
        log.error("Unhandled exception", e);
        String message = messageService.get("global-exception-handler.error.internal");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    // ========== PRODUCT MEDIA PARSE EXCEPTION ==========


    // ========== CELL INACTIVE EXCEPTION ==========
    @ExceptionHandler(CellInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleCellInactive(
            CellInactiveException e) {

        String message = messageService.get("cell.error.inactive", e.getCellId());
        log.warn("CellInactiveException: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // ========== PRODUCT ALREADY IN WAREHOUSE ==========
    @ExceptionHandler(ProductAlreadyInWarehouseException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductAlreadyInWarehouse(
            ProductAlreadyInWarehouseException e) {

        String message = messageService.get(
                "cell.error.product.already.in.warehouse",
                e.getProductId(),
                e.getCellCode()
        );
        log.warn("ProductAlreadyInWarehouseException: {}", message);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException e) {

        String message = e.getMessage();
        log.error("IllegalArgumentException: {}", message);

        // Проверяем, есть ли у нас локализованное сообщение
        if (message == null || message.isEmpty()) {
            message = messageService.get("global-exception-handler.error.invalid.request");
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(SalesOrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> salesOrderNotFoundException(
            SalesOrderNotFoundException e) {

        String message = e.getMessage();
        log.error("SalesOrderNotFoundException: {}", e.getOrderId());

        // Проверяем, есть ли у нас локализованное сообщение
        if (message == null || message.isEmpty()) {
            message = messageService.get("sales.order.not.found", e.getOrderId());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvoiceNotFoundException(
            InvoiceNotFoundException e) {

        String message = e.getLocalizedMessage(messageService);
        log.error("InvoiceNotFoundException: {}", message);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    // GlobalExceptionHandler.java

// =========================================================================
// ОБРАБОТКА ИСКЛЮЧЕНИЙ КАССЫ
// =========================================================================

    /**
     * Обработчик для CashRegisterNotFoundException
     */
    @ExceptionHandler(CashRegisterNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCashRegisterNotFound(
            CashRegisterNotFoundException e) {

        String message = e.getLocalizedMessage(messageService);
        log.error("CashRegisterNotFoundException: {}", message);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * Обработчик для CashRegisterClosedException
     */
    @ExceptionHandler(CashRegisterClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCashRegisterClosed(
            CashRegisterClosedException e) {

        String message = e.getLocalizedMessage(messageService);
        log.warn("CashRegisterClosedException: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Обработчик для InsufficientCashException
     */
    @ExceptionHandler(InsufficientCashException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientCash(
            InsufficientCashException e) {

        String message = e.getLocalizedMessage(messageService);
        log.warn("InsufficientCashException: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }


    // =========================================================================
    // БЛОК 16: ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    private String getValidationMessage(FieldError fieldError) {
        String field = fieldError.getField();
        String code = fieldError.getCode();

        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return switch (field) {
                case "currentPassword" ->
                        messageService.get("global-exception-handler.validation.password.current.required");
                case "newPassword" -> messageService.get("global-exception-handler.validation.password.new.empty");
                case "confirmPassword" ->
                        messageService.get("global-exception-handler.validation.password.confirm.empty");
                default -> messageService.get("global-exception-handler.validation.field.required", field);
            };
        }

        if ("Size".equals(code)) {
            return switch (field) {
                case "currentPassword" ->
                        messageService.get("global-exception-handler.validation.password.current.size", 6);
                case "newPassword" -> messageService.get("global-exception-handler.validation.password.new.size", 6);
                default -> messageService.get("global-exception-handler.validation.field.size", field);
            };
        }

        if ("Pattern".equals(code) && "newPassword".equals(field)) {
            return messageService.get("global-exception-handler.validation.password.new.invalid");
        }

        return messageService.get("global-exception-handler.validation.field.error", field);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return (size / 1024) + " KB";
        if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)) + " MB";
        return (size / (1024 * 1024 * 1024)) + " GB";
    }
}