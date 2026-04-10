package ru.galtor85.household_store.service.currency;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache service for currency data.
 * Provides fast access to currency symbols and exchange rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyCacheService {

    private final CurrencyRepository currencyRepository;

    private final Map<String, String> currencySymbolCache = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> currencyRateCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initCache() {
        refreshCache();
        log.info("Currency cache initialized with {} currencies", currencySymbolCache.size());
    }

    /**
     * Refreshes the entire currency cache from database
     */
    public void refreshCache() {
        currencySymbolCache.clear();
        currencyRateCache.clear();

        currencyRepository.findAll().forEach(currency -> {
            currencySymbolCache.put(currency.getCode(), currency.getSymbol());
            if (currency.getExchangeRate() != null) {
                currencyRateCache.put(currency.getCode(), currency.getExchangeRate());
            }
        });

        log.debug("Currency cache refreshed: {} symbols, {} rates",
                currencySymbolCache.size(), currencyRateCache.size());
    }

    /**
     * Gets currency symbol by code
     *
     * @param currencyCode currency code (e.g., "RUB")
     * @return currency symbol or code as fallback
     */
    public String getCurrencySymbol(String currencyCode) {
        return currencySymbolCache.getOrDefault(currencyCode, currencyCode);
    }

    /**
     * Gets exchange rate by currency code
     *
     * @param currencyCode currency code
     * @return exchange rate or null if not found
     */
    public BigDecimal getExchangeRate(String currencyCode) {
        return currencyRateCache.get(currencyCode);
    }

    /**
     * Updates single currency in cache
     *
     * @param currencyCode currency code
     * @param symbol new symbol
     * @param rate new exchange rate
     */
    public void updateCurrency(String currencyCode, String symbol, BigDecimal rate) {
        currencySymbolCache.put(currencyCode, symbol);
        if (rate != null) {
            currencyRateCache.put(currencyCode, rate);
        }
        log.debug("Currency cache updated for: {}", currencyCode);
    }

    /**
     * Removes currency from cache
     *
     * @param currencyCode currency code
     */
    public void evictCurrency(String currencyCode) {
        currencySymbolCache.remove(currencyCode);
        currencyRateCache.remove(currencyCode);
        log.debug("Currency evicted from cache: {}", currencyCode);
    }
}