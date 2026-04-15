package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.finance.CurrencyCreateRequest;
import ru.galtor85.household_store.dto.request.finance.CurrencyUpdateRequest;
import ru.galtor85.household_store.dto.response.finance.CurrencyDto;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;

/**
 * Converter for Currency entity to/from DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyConverter {

    private final MessageService messageService;

    // =========================================================================
    // ENTITY → DTO
    // =========================================================================

    /**
     * Converts Currency entity to DTO.
     *
     * @param currency the currency entity
     * @return CurrencyDto
     */
    public CurrencyDto toDto(Currency currency) {
        if (currency == null) {
            return null;
        }

        return CurrencyDto.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .localizedName(messageService.get("currency.name." + currency.getCode()))
                .symbol(currency.getSymbol())
                .localizedSymbol(messageService.get("currency.symbol." + currency.getCode()))
                .isBase(currency.getIsBase())
                .exchangeRate(currency.getExchangeRate())
                .decimalPlaces(currency.getDecimalPlaces())
                .isActive(currency.getIsActive())
                .createdBy(currency.getCreatedBy())
                .createdAt(currency.getCreatedAt())
                .updatedAt(currency.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // DTO → ENTITY
    // =========================================================================

    /**
     * Converts CurrencyCreateRequest to Currency entity.
     *
     * @param request   the creation request
     * @param createdBy ID of the user creating the currency
     * @return Currency entity
     */
    public Currency toEntity(CurrencyCreateRequest request, Long createdBy) {
        if (request == null) {
            return null;
        }

        return Currency.builder()
                .code(request.getNormalizedCode())
                .name(request.getNormalizedName())
                .symbol(request.getNormalizedSymbol())
                .isBase(request.isBaseCurrency())
                .exchangeRate(request.getExchangeRate())
                .decimalPlaces(request.getDecimalPlaces())
                .isActive(request.isActive())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // UPDATE EXISTING ENTITY
    // =========================================================================

    /**
     * Updates an existing Currency entity from update request.
     *
     * @param currency the existing currency entity
     * @param request  the update request
     * @return updated Currency entity
     */
    public Currency updateEntity(Currency currency, CurrencyUpdateRequest request) {
        if (currency == null || request == null) {
            return currency;
        }

        if (request.isNameUpdating()) {
            currency.setName(request.getNormalizedName());
        }

        if (request.isSymbolUpdating()) {
            currency.setSymbol(request.getNormalizedSymbol());
        }

        if (request.isExchangeRateUpdating()) {
            currency.setExchangeRate(request.getExchangeRate());
        }

        if (request.isDecimalPlacesUpdating()) {
            currency.setDecimalPlaces(request.getDecimalPlaces());
        }

        if (request.isActiveUpdating()) {
            currency.setIsActive(request.getActiveValue());
        }

        currency.setUpdatedAt(LocalDateTime.now());

        return currency;
    }
}