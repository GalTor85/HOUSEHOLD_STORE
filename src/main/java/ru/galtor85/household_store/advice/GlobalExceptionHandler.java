package ru.galtor85.household_store.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.ApiResponse;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, Locale locale) {
        String message = messageService.get("global-exception-handler.invalid.request");
        log.error("HttpMessageNotReadableException: {} : {}", message, e.getLocalizedMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message + " :" + e.getLocalizedMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.access.denied");
        log.error("AccessDeniedException: {}", message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler({AuthenticationManagerException.class, CustomAuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationExceptions(
            RuntimeException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.authentication");
        String logMessage = messageService.get("global-exception-handler.log.authentication.error", e.getMessage());
        log.error(logMessage);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ValidationRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationRequestException(
            ValidationRequestException e, Locale locale) {

        String message = messageService.get("global-exception-handler.error.validation", e.getMessage());
        String errorLog = messageService.get("global-exception-handler.log.validation.request.exception", message);
        log.error(errorLog);

        if (e.getPrincipal() != null) {
            String warnLog = messageService.get("global-exception-handler.log.validation.request.principal", e.getPrincipal());
            log.warn(warnLog);
        }

        String responseMessage;
        if (e.getPrincipal() != null) {
            responseMessage = messageService.get("global-exception-handler.error.validation.with.principal", message, e.getPrincipal());
        } else {
            responseMessage = message;
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(responseMessage));
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
    public ResponseEntity<ApiResponse<Void>> handleUserAccessException(
            UserAccessException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.access");
        log.error("UserAccessException: {}", message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e, Locale locale) {

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
            message = getValidationMessage(fieldError, locale);
        }

        String errorLog = messageService.get("global-exception-handler.log.validation.field.error", fieldError.getField(), message);
        log.error(errorLog);

        if (fieldError.getRejectedValue() == null) {
            String debugLog = messageService.get("global-exception-handler.log.validation.field.missing", fieldError.getField());
            log.debug(debugLog);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    private String getValidationMessage(FieldError fieldError, Locale locale) {
        String field = fieldError.getField();
        String code = fieldError.getCode();

        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return switch (field) {
                case "currentPassword" -> messageService.get("global-exception-handler.validation.password.current.required");
                case "newPassword" -> messageService.get("global-exception-handler.validation.password.new.empty");
                case "confirmPassword" -> messageService.get("global-exception-handler.validation.password.confirm.empty");
                default -> messageService.get("global-exception-handler.validation.field.required", field);
            };
        }

        if ("Size".equals(code)) {
            return switch (field) {
                case "currentPassword" -> messageService.get("global-exception-handler.validation.password.current.size", 6);
                case "newPassword" -> messageService.get("global-exception-handler.validation.password.new.size", 6);
                default -> messageService.get("global-exception-handler.validation.field.size", field);
            };
        }

        if ("Pattern".equals(code) && "newPassword".equals(field)) {
            return messageService.get("global-exception-handler.validation.password.new.invalid");
        }

        return messageService.get("global-exception-handler.validation.field.error", field);
    }

    @ExceptionHandler({NoSuchMessageException.class, MissingResourceException.class})
    public ResponseEntity<ApiResponse<Void>> handleMessageSourceException(
            Exception e, Locale locale) {
        String logMessage = messageService.get("global-exception-handler.log.message.source.error", e.getMessage());
        log.error(logMessage);
        String userMessage = messageService.get("global-exception-handler.error.configuration");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(userMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception e, Locale locale) {
        log.error("Unhandled exception", e);
        String message = messageService.get("global-exception-handler.error.internal");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserRegistrationError(
            UserRegistrationException e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.register.error", e.getMessage());
        log.error("UserRegistrationException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAuthenticationError.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAuthenticationError(
            UserAuthenticationError e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.login.error", e.getMessage());
        log.error("UserAuthenticationError: {}", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(TokenValidationError.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenValidationError(
            TokenValidationError e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.token.validation.error");
        log.error("TokenValidationError: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserLoginError(
            UserLoginException e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.login.error", e.getMessage());
        log.error("UserLoginException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
            UserAlreadyExistsException e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.register.email.exists", e.getMessage());
        log.error("UserAlreadyExistsException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotActive(
            UserNotActiveException e, Locale locale) {
        String message = messageService.get("global-exception-handler.auth.login.user.inactive");
        log.error("UserNotActiveException: {}", message);
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserFetchedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserFetchedException(
            UserFetchedException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.fetch");
        log.error("UserFetchedException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserCreateException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserCreateException(
            UserCreateException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.operation", e.getMessage());
        log.error("UserCreateException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserUpdateRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserUpdateRoleException(
            UserUpdateRoleException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.operation", e.getMessage());
        log.error("UserUpdateRoleException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserUpdateStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserUpdateStatusException(
            UserUpdateStatusException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.operation", e.getMessage());
        log.error("UserUpdateStatusException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(UserDeleteException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserDeleteException(
            UserDeleteException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.operation", e.getMessage());
        log.error("UserDeleteException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(StatisticException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatisticException(
            StatisticException e, Locale locale) {
        String message = messageService.get("global-exception-handler.error.user.operation", e.getMessage());
        log.error("StatisticException: {}", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotFoundException(
            MessageNotFoundException e, Locale locale) {

        log.error("MessageNotFoundException: {}", e.getMessage());

        String message = messageService.getWithDefault(
                "global-exception-handler.error.configuration.missing.key",
                "Configuration error: missing message key"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }
}