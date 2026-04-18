package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import ru.galtor85.household_store.dto.request.cart.AddToCartRequest;
import ru.galtor85.household_store.dto.request.finance.CashTransactionRequest;
import ru.galtor85.household_store.dto.response.cart.CartDto;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.dto.response.order.SalesOrderDto;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.TransactionType;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.service.cart.CartService;
import ru.galtor85.household_store.service.cash.CashTransactionService;
import ru.galtor85.household_store.service.finance.InvoiceService;
import ru.galtor85.household_store.service.order.SalesOrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Sales Chain Test")
class FullSalesChainTest extends BaseSalesChainTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CashTransactionService cashTransactionService;

    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = createTestData();
    }

    @Test
    @Order(1)
    @DisplayName("TEST-01: Add to cart")
    void testAddToCart() {
        log.info("=== TEST-01: Add to cart ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        CartDto cart = cartService.addToCart(testData.userId(), cartRequest);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        log.info("✅ Cart created: total={}", cart.getTotalAmount());
    }

    @Test
    @Order(2)
    @DisplayName("TEST-02: Create order from cart")
    void testCreateOrderFromCart() {
        log.info("=== TEST-02: Create order from cart ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        cartService.addToCart(testData.userId(), cartRequest);

        SalesOrderDto order = salesOrderService.createOrderFromCart(testData.userId(), "123 Test St, Moscow");
        assertThat(order).isNotNull();
        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        log.info("✅ Order created: number={}", order.getOrderNumber());
    }

    @Test
    @Order(3)
    @DisplayName("TEST-03: Get invoice for order")
    void testGetInvoice() {
        log.info("=== TEST-03: Get invoice for order ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        cartService.addToCart(testData.userId(), cartRequest);
        SalesOrderDto order = salesOrderService.createOrderFromCart(testData.userId(), "123 Test St, Moscow");

        List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(order.getId());
        assertThat(invoices).isNotEmpty();
        InvoiceDto invoice = invoices.getFirst();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        log.info("✅ Invoice created: number={}", invoice.getInvoiceNumber());
    }

    @Test
    @Order(4)
    @DisplayName("TEST-04: Pay invoice via cash register")
    void testPayInvoice() {
        log.info("=== TEST-04: Pay invoice via cash register ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        cartService.addToCart(testData.userId(), cartRequest);
        SalesOrderDto order = salesOrderService.createOrderFromCart(testData.userId(), "123 Test St, Moscow");

        List<InvoiceDto> invoices = invoiceService.getInvoicesBySalesOrder(order.getId());
        assertThat(invoices).isNotEmpty();
        InvoiceDto invoice = invoices.getFirst();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);

        CashTransactionRequest cashRequest = CashTransactionRequest.builder()
                .cashRegisterId(testData.cashRegisterId())
                .transactionType(TransactionType.INCOME)
                .amount(BigDecimal.valueOf(200))
                .currency("RUB")
                .invoiceId(invoice.getId())
                .salesOrderId(order.getId())
                .customerId(testData.userId())
                .description("Payment for order: " + order.getOrderNumber())
                .build();

        CashTransactionDto cashTransaction = cashTransactionService.createTransaction(cashRequest, testData.userId());
        assertThat(cashTransaction).isNotNull();
        assertThat(cashTransaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));

        // Verify invoice was automatically marked as PAID
        InvoiceDto updatedInvoice = invoiceService.getInvoiceById(invoice.getId());
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        log.info("✅ Invoice paid, status: {}", updatedInvoice.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("TEST-05: Update order status to PAID")
    void testUpdateOrderStatusToPaid() {
        log.info("=== TEST-05: Update order status to PAID ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        cartService.addToCart(testData.userId(), cartRequest);
        SalesOrderDto order = salesOrderService.createOrderFromCart(testData.userId(), "123 Test St, Moscow");

        SalesOrderDto paidOrder = salesOrderService.updateOrderStatus(
                order.getId(), "PAID", null, null, testData.userId());
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        log.info("✅ Order status: {}", paidOrder.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("TEST-06: Process order to COMPLETED")
    void testProcessOrderToCompleted() {
        log.info("=== TEST-06: Process order to COMPLETED ===");
        AddToCartRequest cartRequest = new AddToCartRequest(testData.productId(), 2);
        cartService.addToCart(testData.userId(), cartRequest);
        SalesOrderDto order = salesOrderService.createOrderFromCart(testData.userId(), "123 Test St, Moscow");

        salesOrderService.updateOrderStatus(order.getId(), "PAID", null, null, testData.userId());

        SalesOrderDto processing = salesOrderService.updateOrderStatus(order.getId(), "PROCESSING", null, null, testData.userId());
        assertThat(processing.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        SalesOrderDto shipped = salesOrderService.updateOrderStatus(order.getId(), "SHIPPED", "TRACK123", null, testData.userId());
        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        SalesOrderDto delivered = salesOrderService.updateOrderStatus(order.getId(), "DELIVERED", null, null, testData.userId());
        assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);

        SalesOrderDto completed = salesOrderService.updateOrderStatus(order.getId(), "COMPLETED", null, null, testData.userId());
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        log.info("✅ Order completed");
    }
}