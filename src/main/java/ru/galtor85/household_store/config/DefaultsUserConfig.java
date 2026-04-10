package ru.galtor85.household_store.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for default data initialization.
 *
 * <p>Contains default values for admin user, manager user, warehouse,
 * and currency that are created during database initialization.</p>
 *
 * <p>All values are loaded from application.properties with prefix 'app.defaults'.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.defaults")
public class DefaultsUserConfig {

    private AdminConfig admin = new AdminConfig();
    private ManagerConfig manager = new ManagerConfig();
    private WarehouseConfig warehouse = new WarehouseConfig();
    private CurrencyConfig currency = new CurrencyConfig();

    @Data
    public static class AdminConfig {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String role;
    }

    @Data
    public static class ManagerConfig {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String role;
        private String userType;
    }

}