package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.entity.finance.CashRegister;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;

import java.math.BigDecimal;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseSalesChainTest {

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected ProductStockRepository productStockRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected WarehouseRepository warehouseRepository;

    @Autowired
    protected CashRegisterRepository cashRegisterRepository;

    @Autowired
    protected WarehouseConfig warehouseConfig;

    protected record TestData(
            Long userId,
            Long productId,
            Long warehouseId,
            Long cashRegisterId
    ) {}

    protected TestData createTestData() {
        log.info("=== Creating fresh test data ===");

        // Create warehouse (fresh ID)
        Warehouse warehouse = Warehouse.builder()
                .code("WH-TEST-" + System.currentTimeMillis())
                .name("Test Warehouse")
                .address("Test Address")
                .barcode("TEST-BARCODE-" + System.currentTimeMillis())
                .isActive(true)
                .isVisibleForSale(true)
                .totalCapacity(1000)
                .usedCapacity(0)
                .build();
        warehouse = warehouseRepository.save(warehouse);
        Long warehouseId = warehouse.getId();
        warehouseConfig.setDefaultId(warehouseId);
        log.info("Created warehouse with ID: {}", warehouseId);

        // Create cash register
        CashRegister cashRegister = CashRegister.builder()
                .registerNumber("CR-TEST-" + System.currentTimeMillis())
                .name("Test Cash Register")
                .isActive(true)
                .openingBalance(BigDecimal.valueOf(10000))
                .build();
        cashRegister = cashRegisterRepository.save(cashRegister);
        Long cashRegisterId = cashRegister.getId();
        log.info("Created cash register with ID: {}", cashRegisterId);

        // Create user
        User user = User.builder()
                .email("customer-" + System.currentTimeMillis() + "@example.com")
                .firstName("Customer")
                .lastName("Test")
                .mobileNumber("+7999" + System.currentTimeMillis())
                .creator("test")
                .build();
        user = userRepository.save(user);
        Long userId = user.getId();
        log.info("Created user with ID: {}", userId);

        // Create product
        Product product = Product.builder()
                .sku("PROD-TEST-" + System.currentTimeMillis())
                .name("Test Product")
                .price(BigDecimal.valueOf(100))
                .active(true)
                .createdBy("test")
                .build();
        product = productRepository.save(product);
        Long productId = product.getId();
        log.info("Created product with ID: {}", productId);

        // Add stock
        ProductStock stock = ProductStock.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(100)
                .availableQuantity(100)
                .build();
        productStockRepository.save(stock);
        log.info("Added stock for product {} at warehouse {}", productId, warehouseId);

        log.info("=== Test data created ===");

        return new TestData(userId, productId, warehouseId, cashRegisterId);
    }
}