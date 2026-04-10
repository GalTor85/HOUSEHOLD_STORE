package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for financial settings.
 *
 * <p>This class holds all financial configuration including currency defaults,
 * invoice payment terms, and discount percentages for different user types.</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.financial'.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.financial")
public class FinancialConfig {

    private String defaultCurrency;
    private Integer defaultDecimalPlaces;
    private InvoiceConfig invoice = new InvoiceConfig();
    private DiscountsConfig discounts = new DiscountsConfig();

    @Data
    public static class InvoiceConfig {
        private Integer purchaseDueDays;
        private Integer retailDueDays;
        private Integer wholesaleDueDays;
    }

    @Data
    public static class DiscountsConfig {
        private Double wholesale;
        private Double vip;
        private Double partner;
        private Double employee;
    }
}