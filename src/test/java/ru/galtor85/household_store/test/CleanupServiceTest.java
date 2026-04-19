package ru.galtor85.household_store.test;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.finance.Currency;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.payment.PaymentTransaction;
import ru.galtor85.household_store.entity.payment.PaymentTransactionStatus;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.currency.CurrencyRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.payment.PaymentTransactionRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.cleanup.EntityCleanupService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CleanupServiceTest {

    @Autowired
    private EntityCleanupService cleanupService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUserRepository securityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CurrencyRepository currencyRepository;

    private Long adminUserId;
    private Long testSalesOrderId;
    private Long testPurchaseOrderId;
    private Long testInvoiceId;
    private Long testTransactionId;
    private Long testCurrencyId;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder()
                .email("admin-cleanup-" + System.currentTimeMillis() + "@example.com")
                .firstName("Admin")
                .lastName("Cleanup")
                .mobileNumber("+79990000001")
                .creator("system")
                .build();
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();

        SecurityUser adminSecurity = SecurityUser.builder()
                .userId(adminUserId)
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        securityUserRepository.save(adminSecurity);

        SalesOrder salesOrder = SalesOrder.builder()
                .orderNumber("TEST-SO-CLEANUP-" + System.currentTimeMillis())
                .userId(adminUserId)
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(1000))
                .subtotal(BigDecimal.valueOf(1000))
                .createdBy(adminUserId)
                .build();
        salesOrder = salesOrderRepository.save(salesOrder);
        testSalesOrderId = salesOrder.getId();

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .orderNumber("TEST-PO-CLEANUP-" + System.currentTimeMillis())
                .supplierId(1L)
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.valueOf(5000))
                .subtotal(BigDecimal.valueOf(5000))
                .createdBy(adminUserId)
                .build();
        purchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        testPurchaseOrderId = purchaseOrder.getId();

        Invoice invoice = Invoice.builder()
                .invoiceNumber("TEST-INV-CLEANUP-" + System.currentTimeMillis())
                .purchaseOrderId(testPurchaseOrderId)
                .amount(BigDecimal.valueOf(5000))
                .currency("RUB")
                .status(InvoiceStatus.CANCELLED)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .issueDate(LocalDateTime.now().minusMonths(7))
                .createdBy(adminUserId)
                .build();
        invoice = invoiceRepository.save(invoice);
        testInvoiceId = invoice.getId();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .paymentMethodId(1L)
                .invoiceId(testInvoiceId)
                .amount(BigDecimal.valueOf(5000))
                .currency("RUB")
                .status(PaymentTransactionStatus.FAILED)
                .createdBy(adminUserId)
                .build();
        transaction = paymentTransactionRepository.save(transaction);
        testTransactionId = transaction.getId();

        entityManager.createNativeQuery(
                        "UPDATE household_schema.payment_transactions SET created_at = :date WHERE id = :id")
                .setParameter("date", Timestamp.valueOf(LocalDateTime.now().minusMonths(6)))
                .setParameter("id", testTransactionId)
                .executeUpdate();

        entityManager.clear();

        // Create test currency
        Currency currency = Currency.builder()
                .code("TST")
                .name("Test Currency")
                .symbol("T$")
                .isBase(false)
                .exchangeRate(BigDecimal.valueOf(100))
                .decimalPlaces(2)
                .isActive(true)
                .createdBy(adminUserId)
                .build();
        currency = currencyRepository.save(currency);
        testCurrencyId = currency.getId();
    }

    @Test
    @Order(1)
    void testSoftDeleteSalesOrder() {
        cleanupService.softDeleteSalesOrder(testSalesOrderId, "Test deletion", adminUserId);

        SalesOrder deletedOrder = salesOrderRepository.findById(testSalesOrderId).orElse(null);
        assertThat(deletedOrder).isNotNull();
        assertThat(deletedOrder.isDeleted()).isTrue();
        assertThat(deletedOrder.getDeletedAt()).isNotNull();
        assertThat(deletedOrder.getDeletedBy()).isEqualTo(adminUserId);
        assertThat(deletedOrder.getDeleteReason()).isEqualTo("Test deletion");
    }

    @Test
    @Order(2)
    void testSoftDeleteNonExistentSalesOrder() {
        assertThatThrownBy(() -> cleanupService.softDeleteSalesOrder(999999L, "Test", adminUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(3)
    void testSoftDeleteSalesOrderWrongStatus() {
        SalesOrder pendingOrder = SalesOrder.builder()
                .orderNumber("TEST-SO-PENDING-" + System.currentTimeMillis())
                .userId(adminUserId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .subtotal(BigDecimal.valueOf(100))
                .createdBy(adminUserId)
                .build();
        pendingOrder = salesOrderRepository.save(pendingOrder);

        SalesOrder finalPendingOrder = pendingOrder;
        assertThatThrownBy(() -> cleanupService.softDeleteSalesOrder(finalPendingOrder.getId(), "Test", adminUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(4)
    void testSoftDeleteInvoice() {
        cleanupService.softDeleteInvoice(testInvoiceId, "Test deletion", adminUserId);

        Invoice deletedInvoice = invoiceRepository.findById(testInvoiceId).orElse(null);
        assertThat(deletedInvoice).isNotNull();
        assertThat(deletedInvoice.isDeleted()).isTrue();
        assertThat(deletedInvoice.getDeletedAt()).isNotNull();
        assertThat(deletedInvoice.getDeletedBy()).isEqualTo(adminUserId);
        assertThat(deletedInvoice.getDeleteReason()).isEqualTo("Test deletion");
    }

    @Test
    @Order(5)
    void testSoftDeleteInvoiceWrongStatus() {
        Invoice pendingInvoice = Invoice.builder()
                .invoiceNumber("TEST-INV-PENDING-" + System.currentTimeMillis())
                .purchaseOrderId(testPurchaseOrderId)
                .amount(BigDecimal.valueOf(100))
                .currency("RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .issueDate(LocalDateTime.now())
                .createdBy(adminUserId)
                .build();
        pendingInvoice = invoiceRepository.save(pendingInvoice);

        Invoice finalPendingInvoice = pendingInvoice;
        assertThatThrownBy(() -> cleanupService.softDeleteInvoice(finalPendingInvoice.getId(), "Test", adminUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(6)
    void testSoftDeleteNonExistentInvoice() {
        assertThatThrownBy(() -> cleanupService.softDeleteInvoice(999999L, "Test", adminUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(7)
    void testSoftDeletePaymentTransaction() {
        cleanupService.softDeletePaymentTransaction(testTransactionId, "Test deletion", adminUserId);

        PaymentTransaction deletedTransaction = paymentTransactionRepository.findById(testTransactionId).orElse(null);
        assertThat(deletedTransaction).isNotNull();
        assertThat(deletedTransaction.isDeleted()).isTrue();
        assertThat(deletedTransaction.getDeletedAt()).isNotNull();
        assertThat(deletedTransaction.getDeletedBy()).isEqualTo(adminUserId);
        assertThat(deletedTransaction.getDeleteReason()).isEqualTo("Test deletion");
    }

    @Test
    @Order(8)
    void testSoftDeleteCompletedPaymentTransaction() {
        PaymentTransaction completedTransaction = PaymentTransaction.builder()
                .paymentMethodId(1L)
                .invoiceId(testInvoiceId)
                .amount(BigDecimal.valueOf(100))
                .currency("RUB")
                .status(PaymentTransactionStatus.COMPLETED)
                .createdBy(adminUserId)
                .build();
        completedTransaction = paymentTransactionRepository.save(completedTransaction);

        PaymentTransaction finalCompletedTransaction = completedTransaction;
        assertThatThrownBy(() -> cleanupService.softDeletePaymentTransaction(finalCompletedTransaction.getId(), "Test", adminUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(9)
    void testSoftDeleteNonExistentPaymentTransaction() {
        assertThatThrownBy(() -> cleanupService.softDeletePaymentTransaction(999999L, "Test", adminUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(10)
    void testAutoCleanup() {
        cleanupService.softDeleteSalesOrder(testSalesOrderId, "Test", adminUserId);
        cleanupService.softDeleteInvoice(testInvoiceId, "Test", adminUserId);
        cleanupService.softDeletePaymentTransaction(testTransactionId, "Test", adminUserId);

        int deletedCount = cleanupService.cleanupExpiredDeletedEntities();

        assertThat(deletedCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(11)
    void testSoftDeleteAlreadyDeletedEntity() {
        cleanupService.softDeleteSalesOrder(testSalesOrderId, "First", adminUserId);

        assertThatThrownBy(() -> cleanupService.softDeleteSalesOrder(testSalesOrderId, "Second", adminUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Order(12)
    void testSoftDeletedEntitiesStillExist() {
        cleanupService.softDeleteSalesOrder(testSalesOrderId, "Test", adminUserId);

        SalesOrder found = salesOrderRepository.findById(testSalesOrderId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    @Order(13)
    void testSoftDeleteCurrency() {
        cleanupService.softDeleteCurrency(testCurrencyId, "Test deletion", adminUserId);

        Currency deletedCurrency = currencyRepository.findById(testCurrencyId).orElse(null);
        assertThat(deletedCurrency).isNotNull();
        assertThat(deletedCurrency.isDeleted()).isTrue();
        assertThat(deletedCurrency.getDeletedAt()).isNotNull();
        assertThat(deletedCurrency.getDeletedBy()).isEqualTo(adminUserId);
        assertThat(deletedCurrency.getDeleteReason()).isEqualTo("Test deletion");
        assertThat(deletedCurrency.getIsActive()).isFalse();
    }

    @Test
    @Order(14)
    void testSoftDeleteNonExistentCurrency() {
        assertThatThrownBy(() -> cleanupService.softDeleteCurrency(999999L, "Test", adminUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Order(15)
    void testSoftDeleteBaseCurrency() {
        currencyRepository.findByIsBaseTrue().ifPresent(baseCurrency -> assertThatThrownBy(() -> cleanupService.softDeleteCurrency(baseCurrency.getId(), "Test", adminUserId))
                .isInstanceOf(IllegalStateException.class));
    }

    @Test
    @Order(16)
    void testSoftDeleteAlreadyDeletedCurrency() {
        Currency currency = Currency.builder()
                .code("TS2")
                .name("Test Currency 2")
                .symbol("T$")
                .isBase(false)
                .exchangeRate(BigDecimal.valueOf(100))
                .decimalPlaces(2)
                .isActive(true)
                .createdBy(adminUserId)
                .build();
        currency = currencyRepository.save(currency);

        cleanupService.softDeleteCurrency(currency.getId(), "First", adminUserId);

        Currency finalCurrency = currency;
        assertThatThrownBy(() -> cleanupService.softDeleteCurrency(finalCurrency.getId(), "Second", adminUserId))
                .isInstanceOf(IllegalStateException.class);
    }
}