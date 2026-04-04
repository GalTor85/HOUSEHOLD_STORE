package ru.galtor85.household_store.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration properties for currency settings.
 *
 * <p>This class maps properties from {@code application.yml} or {@code application.properties}
 * with the prefix {@code app.currency}. It defines default values for currency-related
 * operations including exchange rates, formatting, and base currency settings.</p>
 *
 * <p>Example configuration in {@code application.yml}:</p>
 * <pre>
 * app:
 *   currency:
 *     default-code: RUB
 *     default-exchange-rate: 1.0000
 *     default-decimal-places: 2
 *     default-active: true
 *     default-base: true
 * </pre>
 *
 * <p>These properties are used when:</p>
 * <ul>
 *   <li>Creating the default currency in the database</li>
 *   <li>Setting up currency conversion defaults</li>
 *   <li>Formatting currency amounts for display</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.currency")
public class CurrencyConfig {

    /**
     * Default currency code (ISO 4217 standard).
     * <p>Examples: RUB, USD, EUR, KZT</p>
     */
    private String defaultCode;

    /**
     * Default exchange rate relative to the base currency.
     * <p>For base currency, this should be 1.0000</p>
     */
    private BigDecimal defaultExchangeRate;

    /**
     * Default number of decimal places for currency formatting.
     * <p>Most currencies use 2 decimal places, some use 0 or 3.</p>
     */
    private Integer defaultDecimalPlaces;

    /**
     * Whether the default currency should be active.
     * <p>Inactive currencies cannot be used in transactions.</p>
     */
    private boolean defaultActive;

    /**
     * Whether the default currency should be the system base currency.
     * <p>Only one currency can be the base currency at any time.</p>
     */
    private boolean defaultBase;
}