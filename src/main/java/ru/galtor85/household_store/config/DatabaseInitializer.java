package ru.galtor85.household_store.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
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
    private final CurrencyConfig currencyConfig;

    @PostConstruct
    @Transactional
    public void init() {
        if (!isTableExists("users")) {
            log.info(messageService.get("database-initializer.log.tables.not.ready"));
            return;
        }



        log.info(messageService.get("database-initializer.log.schema.ready"));

        createDefaultAdmin();
        createDefaultManager();
        createDefaultWarehouse();
        createDefaultCurrency();

        log.info(messageService.get("database-initializer.log.completed"));
    }

    private boolean isTableExists(String tableName) {
        try {
            String schema = "household_schema";
            String query = "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
            Integer result = jdbcTemplate.queryForObject(query, Integer.class, schema, tableName);
            return result != null && result == 1;
        } catch (DataAccessException e) {
            log.debug(messageService.get("database-initializer.log.table.not.exists"), tableName);
            return false;
        }
    }

    private void createDefaultAdmin() {
        try {
            if (userRepository.findByEmail("admin@household.store").isEmpty()) {
                log.info(messageService.get("database-initializer.log.creating.admin"));

                User admin = User.builder()
                        .email("admin@household.store")
                        .firstName(messageService.get("admin-initializer.admin.default.firstname"))
                        .lastName(messageService.get("admin-initializer.admin.default.lastname"))
                        .birthDate(LocalDate.now())
                        .creator(messageService.get("admin-initializer.system"))
                        .build();

                User savedAdmin = userRepository.save(admin);

                SecurityUser adminSecurity = securityUserFactory.createNew(
                        savedAdmin,
                        passwordEncoder.encode("Admin123!"),
                        Role.ADMIN
                );

                securityUserRepository.save(adminSecurity);

                log.info(messageService.get("admin-initializer.log.admin.created",
                        "admin@household.store", "Admin123!"));
            } else {
                log.debug(messageService.get("database-initializer.log.admin.exists"));
            }
        } catch (Exception e) {
            log.error(messageService.get("database-initializer.log.admin.create.failed", e.getMessage()), e);
        }
    }

    private void createDefaultManager() {
        try {
            if (userRepository.findByEmail("manager@household.store").isEmpty()) {
                log.info(messageService.get("database-initializer.log.creating.manager"));

                User manager = User.builder()
                        .email("manager@household.store")
                        .firstName(messageService.get("admin-initializer.manager.default.firstname"))
                        .lastName(messageService.get("admin-initializer.manager.default.lastname"))
                        .birthDate(LocalDate.now())
                        .creator(messageService.get("admin-initializer.system"))
                        .build();

                User savedManager = userRepository.save(manager);

                SecurityUser managerSecurity = securityUserFactory.createNew(
                        savedManager,
                        passwordEncoder.encode("Manager123!"),
                        Role.MANAGER
                );

                securityUserRepository.save(managerSecurity);

                userTypeAssignmentService.assignUserType(
                        savedManager.getId(),
                        UserType.EMPLOYEE,
                        "system",
                        "Default manager user"
                );

                log.info(messageService.get("admin-initializer.log.manager.created",
                        "manager@household.store", "Manager123!"));
            } else {
                log.debug(messageService.get("database-initializer.log.manager.exists"));
            }
        } catch (Exception e) {
            log.error(messageService.get("database-initializer.log.manager.create.failed", e.getMessage()), e);
        }
    }

    private void createDefaultWarehouse() {
        try {
            Long defaultId = warehouseConfig.getDefaultId();
            String defaultName = messageService.get("warehouse.default.name");

            if (!warehouseRepository.existsById(defaultId)) {
                log.info(messageService.get("database-initializer.log.creating.warehouse", defaultName));

                Warehouse defaultWarehouse = Warehouse.builder()
                        .code(warehouseConfig.getDefaultCode())
                        .name(defaultName)
                        .description(messageService.get("warehouse.default.description"))
                        .barcode("WH-DEFAULT-BARCODE-" + System.currentTimeMillis())
                        .barcodeFormat("CODE_128")
                        .address(messageService.get("warehouse.default.address"))
                        .isActive(true)
                        .totalCapacity(1000)
                        .usedCapacity(0)
                        .createdBy(1L)
                        .build();

                warehouseRepository.save(defaultWarehouse);

                log.info(messageService.get("database-initializer.log.warehouse.created", defaultName, defaultId));
            } else {
                log.debug(messageService.get("database-initializer.log.warehouse.exists", defaultName, defaultId));
            }
        } catch (Exception e) {
            log.error(messageService.get("database-initializer.log.warehouse.create.failed", e.getMessage()), e);
        }
    }

    private void createDefaultCurrency() {
        try {
            String defaultCode = currencyConfig.getDefaultCode();
            String defaultName = messageService.get("currency.default.name");
            String defaultSymbol = messageService.get("currency.default.symbol");

            if (!currencyRepository.existsByCode(defaultCode)) {
                log.info(messageService.get("database-initializer.log.creating.currency", defaultCode));

                boolean hasBaseCurrency = currencyRepository.existsByIsBaseTrue();

                Currency defaultCurrency = Currency.builder()
                        .code(defaultCode)
                        .name(defaultName)
                        .symbol(defaultSymbol)
                        .isBase(!hasBaseCurrency)
                        .exchangeRate(currencyConfig.getDefaultExchangeRate())
                        .decimalPlaces(currencyConfig.getDefaultDecimalPlaces())
                        .isActive(currencyConfig.isDefaultActive())
                        .createdBy(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                currencyRepository.save(defaultCurrency);

                String baseSuffix = !hasBaseCurrency ?
                        messageService.get("database-initializer.log.currency.base.suffix") :
                        messageService.get("database-initializer.log.currency.not.base.suffix");

                log.info(messageService.get("database-initializer.log.currency.created", defaultCode, baseSuffix));
            } else {
                log.debug(messageService.get("database-initializer.log.currency.exists", defaultCode));
            }
        } catch (Exception e) {
            log.error(messageService.get("database-initializer.log.currency.create.failed", e.getMessage()), e);
        }
    }

    @Transactional
    public void forceInitialize() {
        log.info(messageService.get("database-initializer.log.force.start"));

        if (!isDatabaseReady()) {
            log.error(messageService.get("database-initializer.log.force.not.ready"));
            return;
        }

        createDefaultAdmin();
        createDefaultManager();
        createDefaultWarehouse();
        createDefaultCurrency();

        log.info(messageService.get("database-initializer.log.force.completed"));
    }

    private boolean isDatabaseReady() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return isTableExists("users") && isTableExists("security_users");
        } catch (Exception e) {
            log.debug(messageService.get("database-initializer.log.database.not.ready", e.getMessage()));
            return false;
        }
    }
}