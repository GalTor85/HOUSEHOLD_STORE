package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.converter.CurrencyConverter;
import ru.galtor85.household_store.dto.CurrencyCreateRequest;
import ru.galtor85.household_store.dto.CurrencyDto;
import ru.galtor85.household_store.dto.CurrencyUpdateRequest;
import ru.galtor85.household_store.entity.Currency;
import ru.galtor85.household_store.processor.CurrencyProcessor;
import ru.galtor85.household_store.repository.CurrencyRepository;
import ru.galtor85.household_store.validator.CurrencyValidator;

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
    // СОЗДАНИЕ
    // =========================================================================

    /**
     * Создает новую валюту
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
    // ПОЛУЧЕНИЕ
    // =========================================================================

    /**
     * Получает список всех валют
     */
    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findAll().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает список активных валют
     */
    @Transactional(readOnly = true)
    public List<CurrencyDto> getActiveCurrencies() {
        return currencyRepository.findByIsActiveTrue().stream()
                .map(converter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает валюту по коду
     */
    @Transactional(readOnly = true)
    public CurrencyDto getCurrencyByCode(String code) {
        validator.validateCode(code);
        Currency currency = validator.validateExists(code);
        validator.validateActive(currency);
        return converter.toDto(currency);
    }

    /**
     * Получает базовую валюту
     */
    @Transactional(readOnly = true)
    public CurrencyDto getBaseCurrency() {
        validator.validateBaseCurrencyExists();
        Currency currency = currencyRepository.findByIsBaseTrue()
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.base.not.found")));
        return converter.toDto(currency);
    }

    // =========================================================================
    // ОБНОВЛЕНИЕ
    // =========================================================================

    /**
     * Обновляет валюту
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
     * Устанавливает базовую валюту
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
     * Обновляет курс обмена
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
     * Активирует/деактивирует валюту
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
    // КОНВЕРТАЦИЯ
    // =========================================================================

    /**
     * Конвертирует сумму из одной валюты в другую
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
     * Форматирует сумму с валютой
     */
    @Transactional(readOnly = true)
    public String formatAmount(BigDecimal amount, String currencyCode) {
        CurrencyDto currency = getCurrencyByCode(currencyCode);
        return currency.format(amount);
    }
}