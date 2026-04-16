package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for stock display settings.
 *
 * <p>This class holds configuration for product stock display including
 * low stock threshold, cache settings, and display preferences.</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.stock.display'.</p>
 *
 * @author G@LTor85
 
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.stock.display")
public class StockDisplayConfig {

    /**
     * Threshold for low stock warning.
     * When available quantity is below this value, status is LOW_STOCK.
     * Default: 10
     */
    private int lowStockThreshold = 10;

    /**
     * Cache duration in minutes for stock availability.
     * Results are cached to reduce database load.
     * Default: 5
     */
    private int cacheMinutes = 5;

    /**
     * Whether to show exact quantity to customers.
     * If false, only status is shown.
     * Default: true
     */
    private boolean showExactQuantity = true;
}