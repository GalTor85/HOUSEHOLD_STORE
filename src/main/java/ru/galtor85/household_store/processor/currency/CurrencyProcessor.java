package ru.galtor85.household_store.processor.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processor for currency operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyProcessor {

    private final CurrencyRepository currencyRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Creates a new currency.
     *
     * @param currency the currency entity to create
     * @return created Currency entity
     */
    @Transactional
    public Currency createCurrency(Currency currency) {
        log.info(logMsg.get("currency.processor.create.start", currency.getCode()));

        Currency saved = currencyRepository.save(currency);

        log.info(logMsg.get("currency.processor.created", saved.getCode()));

        return saved;
    }

    /**
     * Updates an existing currency.
     *
     * @param currency the currency entity with updated values
     * @return updated Currency entity
     */
    @Transactional
    public Currency updateCurrency(Currency currency) {
        log.info(logMsg.get("currency.processor.update.start", currency.getCode()));

        currency.setUpdatedAt(LocalDateTime.now());
        Currency saved = currencyRepository.save(currency);

        log.info(logMsg.get("currency.processor.updated", saved.getCode()));

        return saved;
    }

    /**
     * Sets a currency as the base currency.
     *
     * @param code the currency code
     * @return updated Currency entity
     * @throws IllegalArgumentException if currency not found
     */
    @Transactional
    public Currency setBaseCurrency(String code) {
        log.info(logMsg.get("currency.processor.set.base.start", code));

        currencyRepository.resetBaseCurrency();

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setIsBase(true);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(logMsg.get("currency.processor.set.base.complete", saved.getCode()));

        return saved;
    }

    /**
     * Updates the exchange rate of a currency.
     *
     * @param code the currency code
     * @param rate the new exchange rate
     * @return updated Currency entity
     * @throws IllegalArgumentException if currency not found
     */
    @Transactional
    public Currency updateExchangeRate(String code, BigDecimal rate) {
        log.info(logMsg.get("currency.processor.update.rate.start", code, rate));

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setExchangeRate(rate);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(logMsg.get("currency.processor.update.rate.complete", saved.getCode(), rate));

        return saved;
    }

    /**
     * Toggles the active status of a currency.
     *
     * @param code   the currency code
     * @param active true to activate, false to deactivate
     * @return updated Currency entity
     * @throws IllegalArgumentException if currency not found
     */
    @Transactional
    public Currency toggleActive(String code, boolean active) {
        log.info(logMsg.get("currency.processor.toggle.start", code, active));

        Currency currency = currencyRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("currency.not.found", code)));

        currency.setIsActive(active);
        currency.setUpdatedAt(LocalDateTime.now());

        Currency saved = currencyRepository.save(currency);

        log.info(logMsg.get("currency.processor.toggle.complete", saved.getCode(), active));

        return saved;
    }
}