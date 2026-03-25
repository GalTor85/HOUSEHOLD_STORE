package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Currency;
import ru.galtor85.household_store.repository.CurrencyRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyProcessor {

    private final CurrencyRepository currencyRepository;
    private final MessageService messageService;

    /**
     * Создает новую валюту
     */
    @Transactional
    public Currency createCurrency(Currency currency) {
        log.info(messageService.get("currency.processor.create.start", currency.getCode()));

        Currency saved = currencyRepository.save(currency);

        log.info(messageService.get("currency.processor.created", saved.getCode()));

        return saved;
    }

    /**
     * Обновляет валюту
     */
    @Transactional
    public Currency updateCurrency(Currency currency) {
        log.info(messageService.get("currency.processor.update.start", currency.getCode()));

        currency.setUpdatedAt(LocalDateTime.now());
        Currency saved = currencyRepository.save(currency);

        log.info(messageService.get("currency.processor.updated", saved.getCode()));

        return saved;
    }

    /**
     * Устанавливает базовую валюту
     */
    @Transactional
    public Currency setBaseCurrency(String code) {
        log.info(messageService.get("currency.processor.set.base.start", code));

        // Сбрасываем флаг у всех валют
        currencyRepository.resetBaseCurrency();

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setIsBase(true);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(messageService.get("currency.processor.set.base.complete", saved.getCode()));

        return saved;
    }

    /**
     * Обновляет курс обмена
     */
    @Transactional
    public Currency updateExchangeRate(String code, BigDecimal rate) {
        log.info(messageService.get("currency.processor.update.rate.start", code, rate));

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setExchangeRate(rate);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(messageService.get("currency.processor.update.rate.complete", saved.getCode(), rate));

        return saved;
    }

    /**
     * Активирует/деактивирует валюту
     */
    @Transactional
    public Currency toggleActive(String code, boolean active) {
        log.info(messageService.get("currency.processor.toggle.start", code, active));

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setIsActive(active);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(messageService.get("currency.processor.toggle.complete", saved.getCode(), active));

        return saved;
    }
}