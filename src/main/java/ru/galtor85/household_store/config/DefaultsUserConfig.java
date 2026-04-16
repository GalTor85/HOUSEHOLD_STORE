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
 
 */
@SuppressWarnings("unused")
@Data
@Component
@ConfigurationProperties(prefix = "app.defaults")
public class DefaultsUserConfig {

    private AdminConfig admin = new AdminConfig();
    private ManagerConfig manager = new ManagerConfig();
    private WarehouseConfig warehouse = new WarehouseConfig();
    private FinancialConfig currency = new FinancialConfig();

    @Data
    public static class AdminConfig {
        private String email = "admin@household.store";
        private String password = "Admin123!";
        private String firstName = "Admin";
        private String lastName = "Admin";
        private String role = "ADMIN";
    }

    @Data
    public static class ManagerConfig {
        private String email = "manager@household.store";
        private String password = "Manager123!";
        private String firstName = "Manager";
        private String lastName = "Manager";
        private String role = "MANAGER";
        private String userType = "EMPLOYEE";
    }
}