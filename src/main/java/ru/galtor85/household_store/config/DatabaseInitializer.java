package ru.galtor85.household_store.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;
import ru.galtor85.household_store.util.generator.NumberGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static ru.galtor85.household_store.constants.TechnicalConstants.DEFAULT_REASON_FOR_CREATE;
import static ru.galtor85.household_store.constants.TechnicalConstants.SYSTEM_CREATOR;

/**
 * DatabaseInitializer - Populates the database with default data after Liquibase migrations.
 *
 * <p>This class is responsible for initializing the database with default data required
 * for the application to function properly. It runs after Liquibase migrations have
 * completed and all database tables are created.</p>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>This class does NOT create or modify database schema - only inserts data</li>
 *   <li>All tables must be created by Liquibase migrations before this class executes</li>
 *   <li>Operations are idempotent - safe to run multiple times</li>
 *   <li>Only runs when {@code app.data.initialize=true} and profile is not 'test'</li>
 * </ul>
 *
 * <p><b>Default Data Created:</b></p>
 * <ul>
 *   <li><b>Admin User:</b> admin@household.store with ADMIN role</li>
 *   <li><b>Manager User:</b> manager@household.store with MANAGER role</li>
 *   <li><b>Warehouse:</b> Default warehouse with configurable capacity</li>
 *   <li><b>Currency:</b> RUB as base currency with exchange rate 1.0000</li>
 * </ul>
 *
 * <p><b>Execution Flow:</b></p>
 * <ol>
 *   <li>Application starts and Liquibase runs migrations</li>
 *   <li>Database readiness is verified (tables exist)</li>
 *   <li>Default data is created if missing</li>
 * </ol>
 *
 * @author G@LTor85
 * @version 1.0
 * @see WarehouseConfig
 * @see FinancialConfig
 * @see DefaultsUserConfig
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(name = "app.data.initialize", havingValue = "true")
@Order(1)
public class DatabaseInitializer {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final JdbcTemplate jdbcTemplate;
    private final UserTypeAssignmentService userTypeAssignmentService;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseConfig warehouseConfig;
    private final CurrencyRepository currencyRepository;
    private final FinancialConfig financialConfig;
    private final DefaultsUserConfig defaultsUserConfig;
    private final LogMessageService logMsg;
    private final NumberGenerator numberGenerator;

    /**
     * Initializes default data after application is fully ready.
     * This runs after Liquibase migrations are complete.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(logMsg.get("database-initializer.log.start.ready"));

        if (isDatabaseNotReady()) {
            log.warn(logMsg.get("database-initializer.log.tables.not.ready"));
            return;
        }

        initializeAll();
        log.info(logMsg.get("database-initializer.log.completed"));
    }

    /**
     * Orchestrates the creation of all default data.
     * Calls individual creation methods for admin, manager, warehouse, and currency.
     */
    private void initializeAll() {
        createDefaultAdmin();
        createDefaultWarehouse();
        createDefaultCurrency();
    }

    /**
     * Checks if the database is NOT ready for initialization.
     * Returns true if any required table is missing.
     *
     * @return true if database is not ready, false otherwise
     */
    private boolean isDatabaseNotReady() {
        try {
            return isTableNotExists("users")
                    || isTableNotExists("security_users")
                    || isTableNotExists("warehouses")
                    || isTableNotExists("currencies");
        } catch (Exception e) {
            log.debug(logMsg.get("database-initializer.log.database.not.ready", e.getMessage()));
            return true;
        }
    }

    /**
     * Checks if a specific table does NOT exist in the household_schema.
     *
     * @param tableName the name of the table to check
     * @return true if the table does not exist, false otherwise
     */
    private boolean isTableNotExists(String tableName) {
        try {
            String schema = "household_schema";
            String query = "SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_name = ?";
            Integer result = jdbcTemplate.queryForObject(query, Integer.class, schema, tableName);
            return result == null;
        } catch (DataAccessException e) {
            log.debug(logMsg.get("database-initializer.log.table.not.exists", tableName));
            return true;
        }
    }

    /**
     * Creates the default administrator user if it doesn't exist.
     *
     * <p><b>Default Administrator Credentials:</b></p>
     * <ul>
     *   <li>Email: from configuration (default: admin@household.store)</li>
     *   <li>Password: from configuration (default: Admin123!)</li>
     *   <li>Role: ADMIN</li>
     * </ul>
     *
     * <p><b>Security Warning:</b> This password is for initial setup only.
     * In production environments, it must be changed immediately.</p>
     */
    private void createDefaultAdmin() {
        DefaultsUserConfig.AdminConfig adminConfig = defaultsUserConfig.getAdmin();

        createUserIfNotExists(
                adminConfig.getEmail(),
                adminConfig.getFirstName(),
                adminConfig.getLastName(),
                adminConfig.getPassword(),
                Role.valueOf(adminConfig.getRole()),
                UserType.valueOf(adminConfig.getUserType())
        );
    }

    /**
     * Creates a user if it doesn't already exist in the database.
     *
     * <p>This method handles the complete user creation process including:</p>
     * <ul>
     *   <li>Creating the User entity</li>
     *   <li>Creating the associated SecurityUser for authentication</li>
     *   <li>Optionally assigning a UserType</li>
     * </ul>
     *
     * @param email       user's email address (unique identifier)
     * @param firstName   user's first name
     * @param lastName    user's last name
     * @param rawPassword plain text password (will be encoded)
     * @param role        user's role (ADMIN, MANAGER, USER)
     * @param userType    optional user type (RETAIL, WHOLESALE, VIP, EMPLOYEE, etc.)
     */
    private void createUserIfNotExists(String email, String firstName, String lastName,
                                       String rawPassword, Role role, UserType userType) {
        try {
            if (userRepository.findByEmail(email).isEmpty()) {
                log.info(logMsg.get("database-initializer.log.creating.user", email));

                User user = User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .birthDate(LocalDate.now())
                        .creator(SYSTEM_CREATOR)
                        .build();

                User savedUser = userRepository.save(user);

                SecurityUser securityUser = securityUserFactory.createNew(
                        savedUser,
                        passwordEncoder.encode(rawPassword),
                        role
                );

                securityUserRepository.save(securityUser);

                if (userType != null) {
                    userTypeAssignmentService.assignUserType(
                            savedUser.getId(),
                            userType,
                            SYSTEM_CREATOR,
                            DEFAULT_REASON_FOR_CREATE
                    );
                }

                log.info(logMsg.get("database-initializer.log.user.created", email, role.name()));
            } else {
                log.debug(logMsg.get("database-initializer.log.user.exists", email));
            }
        } catch (Exception e) {
            log.error(logMsg.get("database-initializer.log.user.create.failed", email, e.getMessage()), e);
        }
    }

    /**
     * Retrieves the ID of the admin user.
     *
     * @return admin user ID, or null if not found
     */
    public Long getAdminUserId() {
        String adminEmail = defaultsUserConfig.getAdmin().getEmail();
        return userRepository.findByEmail(adminEmail)
                .map(User::getId)
                .orElse(null);
    }

    /**
     * Creates the default warehouse if it doesn't exist.
     *
     * <p><b>Default Warehouse Properties:</b></p>
     * <ul>
     *   <li>ID: from WarehouseConfig.defaultId (default: 1)</li>
     *   <li>Code: from WarehouseConfig.defaultCode (default: WH-DEFAULT)</li>
     *   <li>Name: localized "Main Warehouse"</li>
     *   <li>Capacity: from WarehouseConfig.defaultCapacity (default: 1000)</li>
     *   <li>Barcode Format: from WarehouseConfig.defaultBarcodeFormat (default: CODE_128)</li>
     * </ul>
     */
    private void createDefaultWarehouse() {
        try {
            Long defaultId = warehouseConfig.getDefaultWarehouseId();
            String defaultName = warehouseConfig.getDefaultWarehouseName();

            if (!warehouseRepository.existsById(defaultId)) {
                log.info(logMsg.get("database-initializer.log.creating.warehouse", defaultName));

                Warehouse defaultWarehouse = Warehouse.builder()
                        .code(warehouseConfig.getDefaultWarehouseCode())
                        .name(defaultName)
                        .description(warehouseConfig.getDefaultWarehouseDescription())
                        .barcode(numberGenerator.generateWarehouseBarcode())
                        .barcodeFormat(warehouseConfig.getDefaultWarehouseBarcodeFormat())
                        .address(warehouseConfig.getDefaultWarehouseAddress())
                        .isActive(true)
                        .totalCapacity(warehouseConfig.getDefaultWarehouseCapacity())
                        .usedCapacity(warehouseConfig.getDefaultWarehouseCapacity())
                        .createdBy(getAdminUserId())
                        .isVisibleForSale(warehouseConfig.getDefaultWarehouseVisibleForSale())
                        .build();

                warehouseRepository.save(defaultWarehouse);

                log.info(logMsg.get("database-initializer.log.warehouse.created", defaultName, defaultId));
            } else {
                log.debug(logMsg.get("database-initializer.log.warehouse.exists", defaultName, defaultId));
            }
        } catch (Exception e) {
            log.error(logMsg.get("database-initializer.log.warehouse.create.failed", e.getMessage()), e);
        }
    }

    /**
     * Creates the default currency if it doesn't exist.
     *
     * <p><b>Default Currency Properties:</b></p>
     * <ul>
     *   <li>Code: from CurrencyConfig.defaultCode (default: RUB)</li>
     *   <li>Name: localized "Russian Ruble"</li>
     *   <li>Symbol: localized "₽"</li>
     *   <li>Exchange Rate: from CurrencyConfig.defaultExchangeRate (default: 1.0000)</li>
     *   <li>Decimal Places: from CurrencyConfig.defaultDecimalPlaces (default: 2)</li>
     * </ul>
     *
     * <p><b>Note:</b> If no base currency exists in the system, this currency
     * automatically becomes the base currency.</p>
     */
    private void createDefaultCurrency() {
        try {
            String defaultCode = financialConfig.getDefaultCurrency();
            String defaultName = messageService.get("currency.default.name");
            String defaultSymbol = messageService.get("currency.default.symbol");

            if (!currencyRepository.existsByCode(defaultCode)) {
                log.info(logMsg.get("database-initializer.log.creating.currency", defaultCode));

                boolean hasBaseCurrency = currencyRepository.existsByIsBaseTrue();

                Currency defaultCurrency = Currency.builder()
                        .code(defaultCode)
                        .name(defaultName)
                        .symbol(defaultSymbol)
                        .isBase(!hasBaseCurrency)
                        .exchangeRate(financialConfig.getDefaultExchangeRate())
                        .decimalPlaces(financialConfig.getDefaultDecimalPlaces())
                        .isActive(financialConfig.isDefaultActive())
                        .createdBy(getAdminUserId())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                currencyRepository.save(defaultCurrency);

                if (!hasBaseCurrency) {
                    log.info(logMsg.get("database-initializer.log.currency.created.base", defaultCode));
                } else {
                    log.info(logMsg.get("database-initializer.log.currency.created.not.base", defaultCode));
                }
            } else {
                log.debug(logMsg.get("database-initializer.log.currency.exists", defaultCode));
            }
        } catch (Exception e) {
            log.error(logMsg.get("database-initializer.log.currency.create.failed", e.getMessage()), e);
        }
    }
}