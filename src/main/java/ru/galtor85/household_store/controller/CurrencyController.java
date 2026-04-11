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
import ru.galtor85.household_store.dto.response.finance.CurrencyDto;
import ru.galtor85.household_store.dto.request.finance.CurrencyCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CurrencyUpdateRequest;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.service.currency.CurrencyService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_CURRENCIES;

/**
 * REST controller for currency management operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Creating new currencies (admin only)</li>
 *   <li>Retrieving all or active currencies</li>
 *   <li>Getting currency by ISO code</li>
 *   <li>Getting the base currency</li>
 *   <li>Updating currency details (admin only)</li>
 *   <li>Setting a currency as base (admin only)</li>
 *   <li>Updating exchange rates (admin only)</li>
 *   <li>Toggling currency active status (admin only)</li>
 *   <li>Converting amounts between currencies</li>
 *   <li>Formatting amounts with currency symbols</li>
 * </ul>
 *
 * <p>All endpoints require authentication. Administrative endpoints
 * require ADMIN role.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(CONTROL_CURRENCIES)
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Currency Management", description = "Endpoints for managing currencies")
public class CurrencyController extends BaseController {

    private final CurrencyService currencyService;
    private final MessageService messageService;

    // =========================================================================
    // CURRENCY CREATION (ADMIN ONLY)
    // =========================================================================

    /**
     * Creates a new currency in the system.
     *
     * <p>This endpoint is only accessible to users with ADMIN role.
     * The currency code must be a valid ISO 4217 three-letter code
     * (e.g., USD, EUR, RUB, KZT).</p>
     *
     * @param request currency creation request with code, name, symbol, exchange rate
     * @return created currency DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new currency",
            description = "Creates a new currency in the system. Admin only.")
    public ResponseEntity<ApiResponse<CurrencyDto>> createCurrency(
            @Valid @RequestBody CurrencyCreateRequest request) {

        Long currentUserId = getCurrentUserId();
        CurrencyDto currency = currencyService.createCurrency(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("currency.created", currency.getCode()),
                        currency));
    }

    // =========================================================================
    // CURRENCY RETRIEVAL
    // =========================================================================

    /**
     * Retrieves a list of all currencies (both active and inactive).
     *
     * @return list of all currency DTOs
     */
    @GetMapping
    @Operation(summary = "Get all currencies",
            description = "Retrieves a list of all currencies including inactive ones")
    public ResponseEntity<ApiResponse<List<CurrencyDto>>> getAllCurrencies() {
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currencies.fetched"),
                currencies));
    }

    /**
     * Retrieves a list of active currencies only.
     *
     * <p>Inactive currencies cannot be used in transactions.</p>
     *
     * @return list of active currency DTOs
     */
    @GetMapping("/active")
    @Operation(summary = "Get active currencies",
            description = "Retrieves a list of currencies that are currently active")
    public ResponseEntity<ApiResponse<List<CurrencyDto>>> getActiveCurrencies() {
        List<CurrencyDto> currencies = currencyService.getActiveCurrencies();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currencies.active.fetched"),
                currencies));
    }

    /**
     * Retrieves a currency by its ISO 4217 code.
     *
     * @param code the currency code (e.g., USD, EUR, RUB)
     * @return currency DTO
     */
    @GetMapping("/{code}")
    @Operation(summary = "Get currency by code",
            description = "Retrieves currency details by ISO 4217 code")
    public ResponseEntity<ApiResponse<CurrencyDto>> getCurrencyByCode(
            @Parameter(description = "Currency code (ISO 4217)", example = "RUB", required = true)
            @PathVariable String code) {

        CurrencyDto currency = currencyService.getCurrencyByCode(code);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.fetched", code),
                currency));
    }

    /**
     * Retrieves the base currency of the system.
     *
     * <p>The base currency is used as the reference for exchange rates
     * and for financial calculations.</p>
     *
     * @return base currency DTO
     */
    @GetMapping("/base")
    @Operation(summary = "Get base currency",
            description = "Retrieves the base currency used for exchange rate calculations")
    public ResponseEntity<ApiResponse<CurrencyDto>> getBaseCurrency() {
        CurrencyDto currency = currencyService.getBaseCurrency();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.base.fetched"),
                currency));
    }

    // =========================================================================
    // CURRENCY UPDATES (ADMIN ONLY)
    // =========================================================================

    /**
     * Updates an existing currency.
     *
     * <p>This endpoint is only accessible to users with ADMIN role.
     * Allows updating name, symbol, exchange rate, decimal places,
     * and active status of a currency.</p>
     *
     * @param code    the currency code to update
     * @param request update request with fields to change
     * @return updated currency DTO
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update currency",
            description = "Updates an existing currency. Admin only.")
    public ResponseEntity<ApiResponse<CurrencyDto>> updateCurrency(
            @Parameter(description = "Currency code (ISO 4217)", example = "RUB", required = true)
            @PathVariable String code,
            @Valid @RequestBody CurrencyUpdateRequest request) {

        CurrencyDto currency = currencyService.updateCurrency(code, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.updated", code),
                currency));
    }

    /**
     * Sets a currency as the system base currency.
     *
     * <p>This endpoint is only accessible to users with ADMIN role.
     * Only one currency can be the base currency at any time.
     * Setting a new base currency will automatically unset the previous one.</p>
     *
     * @param code the currency code to set as base
     * @return updated currency DTO
     */
    @PutMapping("/{code}/base")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set as base currency",
            description = "Sets the specified currency as the system base currency. Admin only.")
    public ResponseEntity<ApiResponse<CurrencyDto>> setBaseCurrency(
            @Parameter(description = "Currency code (ISO 4217)", example = "RUB", required = true)
            @PathVariable String code) {

        CurrencyDto currency = currencyService.setBaseCurrency(code);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.base.set", code),
                currency));
    }

    /**
     * Updates the exchange rate of a currency relative to the base currency.
     *
     * <p>This endpoint is only accessible to users with ADMIN role.
     * The exchange rate determines how much of this currency equals one unit
     * of the base currency.</p>
     *
     * @param code the currency code
     * @param rate the new exchange rate (must be positive)
     * @return updated currency DTO
     */
    @PutMapping("/{code}/rate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update exchange rate",
            description = "Updates the exchange rate of a currency. Admin only.")
    public ResponseEntity<ApiResponse<CurrencyDto>> updateExchangeRate(
            @Parameter(description = "Currency code (ISO 4217)", example = "USD", required = true)
            @PathVariable String code,
            @Parameter(description = "Exchange rate relative to base currency", example = "0.0125", required = true)
            @RequestParam BigDecimal rate) {

        CurrencyDto currency = currencyService.updateExchangeRate(code, rate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.rate.updated", code, rate),
                currency));
    }

    /**
     * Toggles the active status of a currency.
     *
     * <p>This endpoint is only accessible to users with ADMIN role.
     * Inactive currencies cannot be used in new transactions.</p>
     *
     * @param code   the currency code
     * @param active true to activate, false to deactivate
     * @return updated currency DTO
     */
    @PatchMapping("/{code}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle currency active status",
            description = "Activates or deactivates a currency. Admin only.")
    public ResponseEntity<ApiResponse<CurrencyDto>> toggleActive(
            @Parameter(description = "Currency code (ISO 4217)", example = "RUB", required = true)
            @PathVariable String code,
            @Parameter(description = "Active status", example = "true", required = true)
            @RequestParam boolean active) {

        CurrencyDto currency = currencyService.toggleActive(code, active);

        String messageKey = active ? "currency.activated" : "currency.deactivated";
        return ResponseEntity.ok(ApiResponse.success(
                messageService.get(messageKey, code),
                currency));
    }

    // =========================================================================
    // CURRENCY CONVERSION AND FORMATTING
    // =========================================================================

    /**
     * Converts an amount from one currency to another.
     *
     * <p>Uses the stored exchange rates for conversion.
     * Both currencies must be active for conversion to work.</p>
     *
     * @param amount the amount to convert
     * @param from   the source currency code
     * @param to     the target currency code
     * @return converted amount
     */
    @GetMapping("/convert")
    @Operation(summary = "Convert amount between currencies",
            description = "Converts an amount from one currency to another using stored exchange rates")
    public ResponseEntity<ApiResponse<BigDecimal>> convert(
            @Parameter(description = "Amount to convert", example = "1000.00", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Source currency code", example = "RUB", required = true)
            @RequestParam String from,
            @Parameter(description = "Target currency code", example = "USD", required = true)
            @RequestParam String to) {

        BigDecimal result = currencyService.convert(amount, from, to);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.converted", amount, from, result, to),
                result));
    }

    /**
     * Formats an amount with the currency symbol.
     *
     * <p>Returns a string like "1,000.00 ₽" for RUB or
     * "$1,000.00" for USD, respecting the currency's decimal places.</p>
     *
     * @param amount   the amount to format
     * @param currency the currency code
     * @return formatted amount string with currency symbol
     */
    @GetMapping("/format")
    @Operation(summary = "Format amount with currency",
            description = "Formats an amount with the appropriate currency symbol and decimal places")
    public ResponseEntity<ApiResponse<String>> formatAmount(
            @Parameter(description = "Amount to format", example = "1000.00", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Currency code", example = "RUB", required = true)
            @RequestParam String currency) {

        String formatted = currencyService.formatAmount(amount, currency);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.formatted"),
                formatted));
    }
}