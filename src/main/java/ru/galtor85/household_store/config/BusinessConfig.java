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
        private Integer expiryDays=7;
        private Integer maxItems=10;
        private Integer maxQuantityPerItem=5;
        private Integer defaultIncrementValue=1;
    }

    @Data
    public static class StockConfig {
        private Integer lowStockThreshold=5;
        private Integer maxQuantity=100;
    }

    @Data
    public static class PaginationConfig {
        private Integer defaultPage=1;
        private Integer defaultSize=10;
        private Integer maxSize=50;
    }

    @Data
    public static class UserConfig {
        private Integer minPasswordLength=8;
        private Integer minNameLength=2;
        private Integer maxNameLength=50;
        private Integer maxEmailLength=100;
        private Integer maxPhoneLength=20;
        private Integer maxAddressLength=255;
        private Integer maxSurnameLength=50;
        private String allowedSpecialChars="!@#$%^&*()-_=+[]{}|;:'\",.<>?";
    }

    @Data
    public static class BankAccountConfig {
        private Integer maxNameLength=50;
        private Integer maxBankNameLength=50;
    }
}