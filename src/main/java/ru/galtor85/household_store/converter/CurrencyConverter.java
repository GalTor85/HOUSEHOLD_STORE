package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.finance.CurrencyCreateRequest;
import ru.galtor85.household_store.dto.response.finance.CurrencyDto;
import ru.galtor85.household_store.dto.request.finance.CurrencyUpdateRequest;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyConverter {

    private final MessageService messageService;

    // =========================================================================
    // ENTITY → DTO
    // =========================================================================

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

    public List<CurrencyDto> toDtoList(List<Currency> currencies) {
        if (currencies == null) {
            return null;
        }
        return currencies.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // DTO → ENTITY
    // =========================================================================

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
    // ОБНОВЛЕНИЕ СУЩЕСТВУЮЩЕЙ СУЩНОСТИ
    // =========================================================================

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

    // =========================================================================
    // КОНВЕРТАЦИЯ СУММ (CURRENCY)
    // =========================================================================

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (amount == null || from == null || to == null) {
            return amount;
        }

        if (from.getCode().equals(to.getCode())) {
            return amount;
        }

        BigDecimal inBase = from.convertToBase(amount);
        return to.convertFromBase(inBase);
    }

    public BigDecimal convertToBase(BigDecimal amount, Currency currency) {
        if (amount == null || currency == null || currency.getExchangeRate() == null) {
            return amount;
        }
        return amount.multiply(currency.getExchangeRate());
    }

    public BigDecimal convertFromBase(BigDecimal amount, Currency currency) {
        if (amount == null || currency == null || currency.getExchangeRate() == null ||
                currency.getExchangeRate().compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.divide(currency.getExchangeRate(), currency.getDecimalPlaces(),
                RoundingMode.HALF_UP);
    }

    // =========================================================================
    // КОНВЕРТАЦИЯ СУММ (CURRENCY_DTO)
    // =========================================================================

    public BigDecimal convert(BigDecimal amount, CurrencyDto from, CurrencyDto to) {
        if (amount == null || from == null || to == null) {
            return amount;
        }

        if (from.getCode().equals(to.getCode())) {
            return amount;
        }

        BigDecimal inBase = convertToBase(amount, from);
        return convertFromBase(inBase, to);
    }

    public BigDecimal convertToBase(BigDecimal amount, CurrencyDto currency) {
        if (amount == null || currency == null || currency.getExchangeRate() == null) {
            return amount;
        }
        return amount.multiply(currency.getExchangeRate());
    }

    public BigDecimal convertFromBase(BigDecimal amount, CurrencyDto currency) {
        if (amount == null || currency == null || currency.getExchangeRate() == null ||
                currency.getExchangeRate().compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.divide(currency.getExchangeRate(), currency.getDecimalPlaces(),
                RoundingMode.HALF_UP);
    }

    // =========================================================================
    // ФОРМАТИРОВАНИЕ
    // =========================================================================

    public String formatAmount(BigDecimal amount, Currency currency) {
        if (amount == null || currency == null) {
            return "0.00";
        }
        return currency.getFormattedAmount(amount);
    }

    public String formatAmount(BigDecimal amount, CurrencyDto currency) {
        if (amount == null || currency == null) {
            return "0.00";
        }
        return String.format("%,." + currency.getDecimalPlaces() + "f %s",
                amount, currency.getSymbol());
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    public CurrencyDto toSimpleDto(Currency currency) {
        if (currency == null) {
            return null;
        }

        return CurrencyDto.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .exchangeRate(currency.getExchangeRate())
                .decimalPlaces(currency.getDecimalPlaces())
                .isActive(currency.getIsActive())
                .build();
    }

    public CurrencyDto toMinimalDto(Currency currency) {
        if (currency == null) {
            return null;
        }

        return CurrencyDto.builder()
                .code(currency.getCode())
                .symbol(currency.getSymbol())
                .build();
    }
}