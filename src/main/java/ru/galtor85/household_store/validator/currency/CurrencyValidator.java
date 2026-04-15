package ru.galtor85.household_store.validator.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

import static ru.galtor85.household_store.constants.TechnicalConstants.CURRENCY_CODE_LENGTH;

/**
 * Validator for currency operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyValidator {

    private final CurrencyRepository currencyRepository;
    private final MessageService messageService;
    private final LogMessageService logMsg;

    /**
     * Validates currency exists by code.
     *
     * @param code currency code
     * @return currency entity
     * @throws IllegalArgumentException if not found
     */
    public Currency validateExists(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error(logMsg.get("currency.not.found", code));
                    return new IllegalArgumentException(
                            messageService.get("currency.not.found", code)
                    );
                });
    }

    /**
     * Validates currency is active.
     *
     * @param currency currency entity
     * @throws IllegalArgumentException if inactive
     */
    public void validateActive(Currency currency) {
        if (!Boolean.TRUE.equals(currency.getIsActive())) {
            log.error(logMsg.get("currency.inactive", currency.getCode()));
            throw new IllegalArgumentException(
                    messageService.get("currency.inactive", currency.getCode())
            );
        }
    }

    /**
     * Validates currency code format.
     *
     * @param code currency code
     * @throws IllegalArgumentException if invalid format
     */
    public void validateCode(String code) {
        if (code == null || code.length() != CURRENCY_CODE_LENGTH) {
            log.error(logMsg.get("currency.code.invalid", code));
            throw new IllegalArgumentException(
                    messageService.get("currency.code.invalid", code)
            );
        }
    }

    /**
     * Validates currency code is unique.
     *
     * @param code currency code
     * @throws IllegalArgumentException if already exists
     */
    public void validateCodeUnique(String code) {
        if (currencyRepository.existsByCode(code)) {
            log.error(logMsg.get("currency.code.exists", code));
            throw new IllegalArgumentException(
                    messageService.get("currency.code.exists", code)
            );
        }
    }

    /**
     * Validates exchange rate is positive.
     *
     * @param rate exchange rate
     * @throws IllegalArgumentException if invalid
     */
    public void validateExchangeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(logMsg.get("currency.exchange.rate.invalid", rate));
            throw new IllegalArgumentException(
                    messageService.get("currency.exchange.rate.invalid", rate)
            );
        }
    }
}