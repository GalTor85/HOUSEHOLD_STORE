package ru.galtor85.household_store.service.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.finance.CurrencyDto;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private final CurrencyService currencyService;
    private final LogMessageService logMsg;

    /**
     * Converts an amount to the base currency
     *
     * @param amount amount to convert
     * @param currencyCode source currency code
     * @return converted amount in base currency (or original amount if conversion fails)
     */
    @Transactional
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String currencyCode) {
        try {
            CurrencyDto baseCurrency = currencyService.getBaseCurrency();
            if (baseCurrency.getCode().equals(currencyCode)) {
                return amount;
            }
            return currencyService.convert(amount, currencyCode, baseCurrency.getCode());
        } catch (Exception e) {
            log.warn(logMsg.get("currency.conversion.to.base.failed",
                    currencyCode, e.getMessage()));
            return amount;
        }
    }
}