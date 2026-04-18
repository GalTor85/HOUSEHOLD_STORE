package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.common.SalesOrderItemCreateDto;
import ru.galtor85.household_store.dto.request.order.SalesOrderCreateRequest;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrderType;
import ru.galtor85.household_store.service.order.SalesOrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Order Service Tests")
class OrderServiceTest extends BaseSalesChainTest {

    @Autowired
    private SalesOrderService salesOrderService;

    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = createTestData();
    }

    private SalesOrderCreateRequest createOrderRequest(int quantity) {
        SalesOrderItemCreateDto item = SalesOrderItemCreateDto.builder()
                .productId(testData.productId())
                .quantity(quantity)
                .build();

        return SalesOrderCreateRequest.builder()
                .userId(testData.userId())
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
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testData.userId());
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getUserId()).isEqualTo(testData.userId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        log.info("=== TEST-01 PASSED ===");
    }

    @Test
    @DisplayName("TEST-02: Get order by ID - should return correct order")
    void testGetOrderById() {
        log.info("=== TEST-02 START ===");
        SalesOrderCreateRequest request = createOrderRequest(1);
        SalesOrderDto created = salesOrderService.createSalesOrder(request, testData.userId());
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
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testData.userId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        SalesOrderDto updated = salesOrderService.updateOrderStatus(order.getId(), "PAID", null, null, testData.userId());
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        log.info("=== TEST-03 PASSED ===");
    }

    @Test
    @DisplayName("TEST-04: Cancel order - should change status to CANCELLED")
    void testCancelOrder() {
        log.info("=== TEST-04 START ===");
        SalesOrderCreateRequest request = createOrderRequest(1);
        SalesOrderDto order = salesOrderService.createSalesOrder(request, testData.userId());
        SalesOrderDto cancelled = salesOrderService.cancelOrder(order.getId(), "Customer request", testData.userId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Customer request");
        log.info("=== TEST-04 PASSED ===");
    }
}