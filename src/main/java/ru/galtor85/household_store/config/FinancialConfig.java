package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.financial")
public class FinancialConfig {

    private String defaultCurrency = "RUB";
    private Integer defaultDecimalPlaces = 2;
    private InvoiceConfig invoice = new InvoiceConfig();
    private DiscountsConfig discounts = new DiscountsConfig();

    @Data
    public static class InvoiceConfig {
        private Integer purchaseDueDays = 30;
        private Integer retailDueDays = 7;
        private Integer wholesaleDueDays = 30;
    }

    @Data
    public static class DiscountsConfig {
        private Double wholesale = 5.0;
        private Double vip = 10.0;
        private Double partner = 7.0;
        private Double employee = 15.0;
    }
}