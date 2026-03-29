package ru.galtor85.household_store.validator.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyValidator {

    private final CurrencyRepository currencyRepository;
    private final MessageService messageService;

    /**
     * Проверяет существование валюты по коду
     */
    public Currency validateExists(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.error(messageService.get("currency.not.found", code));
                    return new IllegalArgumentException(
                            messageService.get("currency.not.found", code)
                    );
                });
    }

    /**
     * Проверяет, что валюта активна
     */
    public void validateActive(Currency currency) {
        if (!Boolean.TRUE.equals(currency.getIsActive())) {
            log.error(messageService.get("currency.inactive", currency.getCode()));
            throw new IllegalArgumentException(
                    messageService.get("currency.inactive", currency.getCode())
            );
        }
    }

    /**
     * Проверяет корректность кода валюты (длина 3 символа)
     */
    public void validateCode(String code) {
        if (code == null || code.length() != 3) {
            log.error(messageService.get("currency.code.invalid", code));
            throw new IllegalArgumentException(
                    messageService.get("currency.code.invalid", code)
            );
        }
    }

    /**
     * Проверяет уникальность кода валюты
     */
    public void validateCodeUnique(String code) {
        if (currencyRepository.existsByCode(code)) {
            log.error(messageService.get("currency.code.exists", code));
            throw new IllegalArgumentException(
                    messageService.get("currency.code.exists", code)
            );
        }
    }

    /**
     * Проверяет, что базовая валюта существует
     */
    public void validateBaseCurrencyExists() {
        if (!currencyRepository.existsByIsBaseTrue()) {
            log.error(messageService.get("currency.base.not.found"));
            throw new IllegalStateException(
                    messageService.get("currency.base.not.found")
            );
        }
    }

    /**
     * Проверяет курс обмена
     */
    public void validateExchangeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("currency.exchange.rate.invalid", rate));
            throw new IllegalArgumentException(
                    messageService.get("currency.exchange.rate.invalid", rate)
            );
        }
    }
}