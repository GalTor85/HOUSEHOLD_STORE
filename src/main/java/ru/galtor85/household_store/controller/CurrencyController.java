package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.service.CurrencyService;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Currency Management", description = "Endpoints for managing currencies")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final MessageService messageService;

    // =========================================================================
    // СОЗДАНИЕ
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new currency")
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
    // ПОЛУЧЕНИЕ
    // =========================================================================

    @GetMapping
    @Operation(summary = "Get all currencies")
    public ResponseEntity<ApiResponse<List<CurrencyDto>>> getAllCurrencies() {
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currencies.fetched"),
                currencies));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active currencies")
    public ResponseEntity<ApiResponse<List<CurrencyDto>>> getActiveCurrencies() {
        List<CurrencyDto> currencies = currencyService.getActiveCurrencies();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currencies.active.fetched"),
                currencies));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get currency by code")
    public ResponseEntity<ApiResponse<CurrencyDto>> getCurrencyByCode(
            @PathVariable String code) {

        CurrencyDto currency = currencyService.getCurrencyByCode(code);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.fetched", code),
                currency));
    }

    @GetMapping("/base")
    @Operation(summary = "Get base currency")
    public ResponseEntity<ApiResponse<CurrencyDto>> getBaseCurrency() {
        CurrencyDto currency = currencyService.getBaseCurrency();

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.base.fetched"),
                currency));
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ
    // =========================================================================

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update currency")
    public ResponseEntity<ApiResponse<CurrencyDto>> updateCurrency(
            @PathVariable String code,
            @Valid @RequestBody CurrencyUpdateRequest request) {

        CurrencyDto currency = currencyService.updateCurrency(code, request);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.updated", code),
                currency));
    }

    @PutMapping("/{code}/base")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set as base currency")
    public ResponseEntity<ApiResponse<CurrencyDto>> setBaseCurrency(
            @PathVariable String code) {

        CurrencyDto currency = currencyService.setBaseCurrency(code);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.base.set", code),
                currency));
    }

    @PutMapping("/{code}/rate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update exchange rate")
    public ResponseEntity<ApiResponse<CurrencyDto>> updateExchangeRate(
            @PathVariable String code,
            @RequestParam BigDecimal rate) {

        CurrencyDto currency = currencyService.updateExchangeRate(code, rate);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.rate.updated", code, rate),
                currency));
    }

    @PatchMapping("/{code}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle currency active status")
    public ResponseEntity<ApiResponse<CurrencyDto>> toggleActive(
            @PathVariable String code,
            @RequestParam boolean active) {

        CurrencyDto currency = currencyService.toggleActive(code, active);

        String messageKey = active ? "currency.activated" : "currency.deactivated";
        return ResponseEntity.ok(ApiResponse.success(
                messageService.get(messageKey, code),
                currency));
    }

    // =========================================================================
    // КОНВЕРТАЦИЯ
    // =========================================================================

    @GetMapping("/convert")
    @Operation(summary = "Convert amount between currencies")
    public ResponseEntity<ApiResponse<BigDecimal>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {

        BigDecimal result = currencyService.convert(amount, from, to);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.converted", amount, from, result, to),
                result));
    }

    @GetMapping("/format")
    @Operation(summary = "Format amount with currency")
    public ResponseEntity<ApiResponse<String>> formatAmount(
            @RequestParam BigDecimal amount,
            @RequestParam String currency) {

        String formatted = currencyService.formatAmount(amount, currency);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("currency.formatted"),
                formatted));
    }

    private Long getCurrentUserId() {
        // TODO: Получить ID текущего пользователя из SecurityContext
        return 1L;
    }
}