package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for business logic settings.
 *
 * <p>All values are loaded from application.properties with prefix 'app.business'.
 * Default values are set in properties files, not in code.</p>
 *
 * @author G@LTor85
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.business")
public class BusinessConfig {

    private CartConfig cart = new CartConfig();
    private StockConfig stock = new StockConfig();
    private PaginationConfig pagination = new PaginationConfig();
    private UserConfig user = new UserConfig();
    private BankAccountConfig BankAccount = new BankAccountConfig();

    @Data
    public static class CartConfig {
        private Integer expiryDays;
        private Integer maxItems;
        private Integer maxQuantityPerItem;
        private Integer defaultIncrementValue;
    }

    @Data
    public static class StockConfig {
        private Integer lowStockThreshold;
        private Integer maxQuantity;
    }

    @Data
    public static class PaginationConfig {
        private Integer defaultPage;
        private Integer defaultSize;
        private Integer maxSize;
    }

    @Data
    public static class UserConfig {
        private Integer minPasswordLength;
        private Integer minNameLength;
        private Integer maxNameLength;
        private Integer maxEmailLength;
        private Integer maxPhoneLength;
        private Integer maxAddressLength;
        private Integer maxSurnameLength;
        private String allowedSpecialChars;
    }

    @Data
    public static class BankAccountConfig {
        private Integer maxNameLength;
        private Integer maxBankNameLength;
    }
}