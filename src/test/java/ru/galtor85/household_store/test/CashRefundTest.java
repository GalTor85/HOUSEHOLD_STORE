package ru.galtor85.household_store.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.cash.CashTransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cash Refund Tests")
class CashRefundTest {

    @Autowired
    private CashTransactionService cashTransactionService;

    @Autowired
    private CashTransactionRepository cashTransactionRepository;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CashRegisterService cashRegisterService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    private CashRegister cashRegister;
    private Long testSupplierId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        cashRegister = CashRegister.builder()
                .registerNumber("TEST-001")
                .name("Test Cash Register")
                .isActive(true)
                .openingBalance(BigDecimal.valueOf(10000))
                .openedAt(LocalDateTime.now())
                .build();
        cashRegister = cashRegisterRepository.save(cashRegister);

        // Create test supplier
        Supplier supplier = Supplier.builder()
                .name("Test Supplier")
                .status(ru.galtor85.household_store.entity.supplier.SupplierStatus.ACTIVE)
                .build();
        supplier = supplierRepository.save(supplier);
        testSupplierId = supplier.getId();

        // Create test user
        User user = User.builder()
                .email("test-" + System.currentTimeMillis() + "@example.com")
                .firstName("Test")
                .lastName("User")
                .creator("test")
                .build();
        user = userRepository.save(user);
        testUserId = user.getId();
    }

    private PurchaseOrder createTestPurchaseOrder(BigDecimal amount) {
        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber("TEST-PO-" + System.currentTimeMillis())
                .supplierId(testSupplierId)
                .status(OrderStatus.PENDING)
                .totalAmount(amount)
                .subtotal(amount)
                .createdBy(1L)
                .build();
        return purchaseOrderRepository.save(order);
    }

    private SalesOrder createTestSalesOrder(BigDecimal amount) {
        SalesOrder order = SalesOrder.builder()
                .orderNumber("TEST-SO-" + System.currentTimeMillis())
                .userId(testUserId)
                .status(OrderStatus.PENDING)
                .totalAmount(amount)
                .subtotal(amount)
                .createdBy(1L)
                .build();
        return salesOrderRepository.save(order);
    }

    private Invoice createTestInvoice(OrderType orderType, BigDecimal amount) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("TEST-INV-" + System.currentTimeMillis())
                .amount(amount)
                .currency("RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .issueDate(LocalDateTime.now())
                .createdBy(1L)
                .build();

        if (orderType == OrderType.SALES) {
            SalesOrder order = createTestSalesOrder(amount);
            invoice.setSalesOrderId(order.getId());
        } else {
            PurchaseOrder order = createTestPurchaseOrder(amount);
            invoice.setPurchaseOrderId(order.getId());
        }

        return invoiceRepository.save(invoice);
    }

    private CashTransaction createTestTransaction(CashRegister cashRegister, Invoice invoice,
                                                  TransactionType type, BigDecimal amount) {
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        BigDecimal balanceAfter;
        if (type == TransactionType.INCOME) {
            balanceAfter = balanceBefore.add(amount);
        } else if (type == TransactionType.EXPENSE) {
            balanceAfter = balanceBefore.subtract(amount);
        } else {
            balanceAfter = balanceBefore;
        }

        CashTransaction transaction = CashTransaction.builder()
                .cashRegister(cashRegister)
                .invoice(invoice)
                .transactionType(type)
                .amount(amount)
                .currency("RUB")
                .cashierId(1L)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        CashTransaction saved = cashTransactionRepository.save(transaction);

        if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }

        return saved;
    }

    // =========================================================================
    // PROPORTIONAL REFUND CALCULATION TESTS
    // =========================================================================

    @Test
    @DisplayName("TEST-01: Calculate proportional refund for supplier (2 payments)")
    void testCalculateProportionalRefundForSupplier() {
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        CashTransaction payment1 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));
        CashTransaction payment2 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(4000));

        BigDecimal refundAmount = BigDecimal.valueOf(3000);

        List<CashTransactionService.ProportionalRefundItem> calculations =
                cashTransactionService.calculateProportionalRefunds(purchaseInvoice.getId(), refundAmount);

        assertThat(calculations).hasSize(2);
        assertThat(calculations.get(0).originalTransactionId()).isEqualTo(payment1.getId());
        assertThat(calculations.get(0).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1800));
        assertThat(calculations.get(1).originalTransactionId()).isEqualTo(payment2.getId());
        assertThat(calculations.get(1).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    @DisplayName("TEST-02: Calculate proportional refund for customer (2 payments)")
    void testCalculateProportionalRefundForCustomer() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(5000));

        CashTransaction payment1 = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(3000));
        CashTransaction payment2 = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(2000));

        BigDecimal refundAmount = BigDecimal.valueOf(2500);

        List<CashTransactionService.ProportionalRefundItem> calculations =
                cashTransactionService.calculateProportionalRefunds(salesInvoice.getId(), refundAmount);

        assertThat(calculations).hasSize(2);
        assertThat(calculations.get(0).originalTransactionId()).isEqualTo(payment1.getId());
        assertThat(calculations.get(0).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(calculations.get(1).originalTransactionId()).isEqualTo(payment2.getId());
        assertThat(calculations.get(1).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    // =========================================================================
    // EXECUTE PROPORTIONAL REFUND TESTS
    // =========================================================================

    @Test
    @DisplayName("TEST-03: Execute proportional refund for supplier")
    void testExecuteProportionalRefundForSupplier() {
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(4000));

        BigDecimal refundAmount = BigDecimal.valueOf(3000);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        List<CashTransactionDto> refunds = cashTransactionService.executeProportionalRefund(
                purchaseInvoice.getId(), refundAmount, "Test proportional refund", 1L);

        assertThat(refunds).hasSize(2);
        assertThat(refunds.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1800));
        assertThat(refunds.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));

        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.add(refundAmount));

        for (CashTransactionDto refund : refunds) {
            assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        }
    }

    @Test
    @DisplayName("TEST-04: Execute proportional refund for customer")
    void testExecuteProportionalRefundForCustomer() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(5000));

        createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(3000));
        createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(2000));

        BigDecimal refundAmount = BigDecimal.valueOf(2500);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        List<CashTransactionDto> refunds = cashTransactionService.executeProportionalRefund(
                salesInvoice.getId(), refundAmount, "Test proportional refund", 1L);

        assertThat(refunds).hasSize(2);
        assertThat(refunds.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(refunds.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(refundAmount));
    }

    // =========================================================================
    // VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("TEST-05: Refund amount exceeds paid amount")
    void testRefundAmountExceedsPaidThrowsException() {
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));

        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                purchaseInvoice.getId(), BigDecimal.valueOf(7000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TEST-06: No payments found for invoice")
    void testNoPaymentsThrowsException() {
        Invoice invoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                invoice.getId(), BigDecimal.valueOf(1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TEST-07: Zero refund amount")
    void testZeroRefundAmountThrowsException() {
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));

        assertThatThrownBy(() -> cashTransactionService.executeProportionalRefund(
                purchaseInvoice.getId(), BigDecimal.ZERO, "Zero refund", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TEST-08: Invoice not found")
    void testInvoiceNotFoundThrowsException() {
        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                99999L, BigDecimal.valueOf(1000)))
                .isInstanceOf(ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException.class);
    }

    // =========================================================================
    // SINGLE TRANSACTION REFUND TESTS
    // =========================================================================

    @Test
    @DisplayName("TEST-09: Create single partial refund")
    void testCreatePartialRefund() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        BigDecimal refundAmount = BigDecimal.valueOf(300);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        CashTransactionDto refund = cashTransactionService.createPartialRefund(
                payment.getId(), refundAmount, "Partial refund", 1L);

        assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getAmount()).isEqualByComparingTo(refundAmount);

        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(refundAmount));
    }

    @Test
    @DisplayName("TEST-10: Full transaction refund")
    void testCancelTransaction() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        CashTransactionDto refund = cashTransactionService.cancelTransaction(
                payment.getId(), "Full refund", 1L);

        assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(BigDecimal.valueOf(1000)));
    }

    @Test
    @DisplayName("TEST-11: Double refund prevention")
    void testDoubleRefundThrowsException() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        cashTransactionService.cancelTransaction(payment.getId(), "First refund", 1L);

        assertThatThrownBy(() -> cashTransactionService.cancelTransaction(payment.getId(), "Second refund", 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("TEST-12: Invoice status becomes REFUNDED after full refund")
    void testInvoiceStatusAfterFullRefund() {
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        cashTransactionService.cancelTransaction(payment.getId(), "Full refund", 1L);

        Invoice updatedInvoice = invoiceRepository.findById(salesInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
    }
}