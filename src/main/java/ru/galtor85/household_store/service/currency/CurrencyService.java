package ru.galtor85.household_store.service.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.converter.CurrencyConverter;
import ru.galtor85.household_store.dto.request.finance.CurrencyCreateRequest;
import ru.galtor85.household_store.dto.response.finance.CurrencyDto;
import ru.galtor85.household_store.dto.request.finance.CurrencyUpdateRequest;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.processor.currency.CurrencyProcessor;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.currency.CurrencyValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyValidator validator;
    private final CurrencyProcessor processor;
    private final CurrencyConverter converter;
    private final MessageService messageService;

    // =========================================================================
    // CREATION
    // =========================================================================

    /**
     * Creates a new currency
     */
    @Transactional
    public CurrencyDto createCurrency(CurrencyCreateRequest request, Long createdBy) {
        log.info(messageService.get("currency.service.create.start", request.getCode()));

        validator.validateCode(request.getCode());
        validator.validateCodeUnique(request.getCode());
        validator.validateExchangeRate(request.getExchangeRate());

        Currency currency = converter.toEntity(request, createdBy);
        Currency saved = processor.createCurrency(currency);

        log.info(messageService.get("currency.service.created", saved.getCode()));

        return converter.toDto(saved);
    }

    // =========================================================================
    // RETRIEVAL
    // =========================================================================

    /**
     * Retrieves all currencies
     */
    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findAll().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves active currencies only
     */
    @Transactional(readOnly = true)
    public List<CurrencyDto> getActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves currency by code
     */
    @Transactional(readOnly = true)
    public CurrencyDto getCurrencyByCode(String code) {
        validator.validateCode(code);
        Currency currency = validator.validateExists(code);
        validator.validateActive(currency);
        return converter.toDto(currency);
    }

    /**
     * Retrieves the base currency
     */
    @Transactional(readOnly = true)
    public CurrencyDto getBaseCurrency() {
        Currency currency = currencyRepository.findByIsBaseTrue()
                .orElseThrow(() -> new IllegalStateException(
                        messageService.get("currency.base.not.found")
                ));
        return converter.toDto(currency);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Updates a currency
     */
    @Transactional
    public CurrencyDto updateCurrency(String code, CurrencyUpdateRequest request) {
        log.info(messageService.get("currency.service.update.start", code));

        validator.validateCode(code);
        Currency existing = validator.validateExists(code);

        if (request.getExchangeRate() != null) {
            validator.validateExchangeRate(request.getExchangeRate());
        }

        Currency updated = processor.updateCurrency(
                converter.updateEntity(existing, request));

        log.info(messageService.get("currency.service.updated", updated.getCode()));

        return converter.toDto(updated);
    }

    /**
     * Sets a currency as the base currency
     */
    @Transactional
    public CurrencyDto setBaseCurrency(String code) {
        log.info(messageService.get("currency.service.set.base.start", code));

        validator.validateCode(code);
        validator.validateExists(code);

        Currency currency = processor.setBaseCurrency(code);

        log.info(messageService.get("currency.service.set.base.complete", currency.getCode()));

        return converter.toDto(currency);
    }

    /**
     * Updates the exchange rate
     */
    @Transactional
    public CurrencyDto updateExchangeRate(String code, BigDecimal rate) {
        log.info(messageService.get("currency.service.update.rate.start", code, rate));

        validator.validateCode(code);
        validator.validateExists(code);
        validator.validateExchangeRate(rate);

        Currency currency = processor.updateExchangeRate(code, rate);

        log.info(messageService.get("currency.service.update.rate.complete",
                currency.getCode(), rate));

        return converter.toDto(currency);
    }

    /**
     * Activates/deactivates a currency
     */
    @Transactional
    public CurrencyDto toggleActive(String code, boolean active) {
        log.info(messageService.get("currency.service.toggle.start", code, active));

        validator.validateCode(code);
        validator.validateExists(code);

        Currency currency = processor.toggleActive(code, active);

        log.info(messageService.get("currency.service.toggle.complete",
                currency.getCode(), active));

        return converter.toDto(currency);
    }

    // =========================================================================
    // CONVERSION
    // =========================================================================

    /**
     * Converts an amount from one currency to another
     */
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String fromCode, String toCode) {
        if (fromCode.equals(toCode)) {
            return amount;
        }

        Currency from = validator.validateExists(fromCode);
        Currency to = validator.validateExists(toCode);

        BigDecimal inBase = from.convertToBase(amount);
        return to.convertFromBase(inBase);
    }

    /**
     * Converts amount to base currency for storage
     *
     * @param amount       amount to convert
     * @param currencyCode source currency code
     * @return amount in base currency
     */
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String currencyCode) {
        try {
            CurrencyDto baseCurrency = getBaseCurrency();
            if (baseCurrency.getCode().equals(currencyCode)) {
                return amount;
            }
            return convert(amount, currencyCode, baseCurrency.getCode());
        } catch (Exception e) {
            log.warn(messageService.get("currency.conversion.to.base.failed",
                    currencyCode, e.getMessage()));
            return amount;
        }
    }

    /**
     * Formats an amount with the currency symbol
     */
    @Transactional(readOnly = true)
    public String formatAmount(BigDecimal amount, String currencyCode) {
        CurrencyDto currency = getCurrencyByCode(currencyCode);
        return currency.format(amount);
    }
}