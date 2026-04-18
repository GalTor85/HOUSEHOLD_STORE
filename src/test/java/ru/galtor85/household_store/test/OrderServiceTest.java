package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.WarehouseConfig;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.warehouse.Warehouse;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.warehouse.WarehouseRepository;
import ru.galtor85.household_store.service.order.SalesOrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Order Service Tests")
class OrderServiceTest {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductStockRepository productStockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseConfig warehouseConfig;

    private Long testUserId;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        log.info("=== SETUP: Creating test data ===");

        // Create warehouse WITHOUT
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
        Long testWarehouseId = warehouse.getId();
        log.info("Created warehouse with ID: {}", testWarehouseId);

        // Set this ID as default in WarehouseConfig
        warehouseConfig.setDefaultId(testWarehouseId);

        // Verify it worked
        log.info("warehouseConfig.getDefaultWarehouseId() = {}", warehouseConfig.getDefaultWarehouseId());

        // Create test user
        User testUser = User.builder()
                .email("customer-order-" + System.currentTimeMillis() + "@example.com")
                .firstName("Customer")
                .lastName("Order")
                .mobileNumber("+79991234567")
                .creator("test")
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
        log.info("Created customer user with ID: {}", testUserId);

        // Create test product
        Product product = Product.builder()
                .sku("TEST-PRODUCT-" + System.currentTimeMillis())
                .name("Test Product")
                .price(BigDecimal.valueOf(100))
                .active(true)
                .createdBy("test")
                .build();
        product = productRepository.save(product);
        testProductId = product.getId();
        log.info("Created product with ID: {}, price: {}", testProductId, product.getPrice());

        // Add stock
        ProductStock stock = ProductStock.builder()
                .productId(testProductId)
                .warehouseId(testWarehouseId)
                .quantity(100)
                .availableQuantity(100)
                .build();
        productStockRepository.save(stock);
        log.info("Added stock: quantity=100 for product {} at warehouse {}", testProductId, testWarehouseId);

        log.info("=== SETUP COMPLETE ===");
    }

    private SalesOrderCreateRequest createOrderRequest(int quantity) {
        SalesOrderItemCreateDto item = SalesOrderItemCreateDto.builder()
                .productId(testProductId)
                .quantity(quantity)
                .build();

        return SalesOrderCreateRequest.builder()
                .userId(testUserId)
                .items(List.of(item))
                .orderType(SalesOrderType.RETAIL)
                .shippingAddress("123 Test St, Moscow")
                .billingAddress("123 Test St, Moscow")
                .paymentMethod("CARD")
                .build();
    }

    @Test
    @DisplayName("TEST-01: Create order - should create successfully")
    void testCreateOrder() {
        log.info("=== TEST-01 START ===");
        SalesOrderCreateRequest request = createOrderRequest(2);
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testUserId);
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getUserId()).isEqualTo(testUserId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        log.info("=== TEST-01 PASSED ===");
    }

    @Test
    @DisplayName("TEST-02: Get order by ID - should return correct order")
    void testGetOrderById() {
        log.info("=== TEST-02 START ===");
        SalesOrderCreateRequest request = createOrderRequest(1);
        SalesOrderDto created = salesOrderService.createSalesOrder(request, testUserId);
        SalesOrderDto found = salesOrderService.getSalesOrderById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getOrderNumber()).isEqualTo(created.getOrderNumber());
        log.info("=== TEST-02 PASSED ===");
    }

    @Test
    @DisplayName("TEST-03: Update order status - should change status")
    void testUpdateOrderStatus() {
        log.info("=== TEST-03 START ===");
        SalesOrderCreateRequest request = createOrderRequest(1);
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testUserId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        SalesOrderDto updated = salesOrderService.updateOrderStatus(order.getId(), "PAID", null, null, testUserId);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        log.info("=== TEST-03 PASSED ===");
    }

    @Test
    @DisplayName("TEST-04: Cancel order - should change status to CANCELLED")
    void testCancelOrder() {
        log.info("=== TEST-04 START ===");
        SalesOrderCreateRequest request = createOrderRequest(1);
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testUserId);
        SalesOrderDto cancelled = salesOrderService.cancelOrder(order.getId(), "Customer request", testUserId);
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Customer request");
        log.info("=== TEST-04 PASSED ===");
    }
}