package ru.galtor85.household_store.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.finance.CashTransactionDto;
import ru.galtor85.household_store.entity.finance.*;
import ru.galtor85.household_store.entity.order.OrderType;
import ru.galtor85.household_store.repository.cash.CashRegisterRepository;
import ru.galtor85.household_store.repository.cash.CashTransactionRepository;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.service.cash.CashRegisterService;
import ru.galtor85.household_store.service.cash.CashTransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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

    private CashRegister cashRegister;

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
    }

    // =========================================================================
    // PROPORTIONAL REFUND CALCULATION TESTS
    // =========================================================================

    // TEST-01: Calculate proportional refund for supplier (2 payments)
    @Test
    @DisplayName("Should calculate proportional refund correctly for supplier")
    void testCalculateProportionalRefundForSupplier() {
        // Given
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        CashTransaction payment1 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));
        CashTransaction payment2 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(4000));

        BigDecimal refundAmount = BigDecimal.valueOf(3000);

        // When
        List<CashTransactionService.ProportionalRefundItem> calculations =
                cashTransactionService.calculateProportionalRefunds(purchaseInvoice.getId(), refundAmount);

        // Then
        assertThat(calculations).hasSize(2);
        assertThat(calculations.get(0).originalTransactionId()).isEqualTo(payment1.getId());
        assertThat(calculations.get(0).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1800));
        assertThat(calculations.get(1).originalTransactionId()).isEqualTo(payment2.getId());
        assertThat(calculations.get(1).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    // TEST-02: Calculate proportional refund for customer (2 payments)
    @Test
    @DisplayName("Should calculate proportional refund correctly for customer")
    void testCalculateProportionalRefundForCustomer() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(5000));

        CashTransaction payment1 = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(3000));
        CashTransaction payment2 = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(2000));

        BigDecimal refundAmount = BigDecimal.valueOf(2500);

        // When
        List<CashTransactionService.ProportionalRefundItem> calculations =
                cashTransactionService.calculateProportionalRefunds(salesInvoice.getId(), refundAmount);

        // Then
        assertThat(calculations).hasSize(2);
        assertThat(calculations.get(0).originalTransactionId()).isEqualTo(payment1.getId());
        assertThat(calculations.get(0).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(calculations.get(1).originalTransactionId()).isEqualTo(payment2.getId());
        assertThat(calculations.get(1).refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    // =========================================================================
    // EXECUTE PROPORTIONAL REFUND TESTS
    // =========================================================================

    // TEST-03: Execute proportional refund for supplier (balance increases)
    @Test
    @DisplayName("Should execute proportional refund for supplier")
    void testExecuteProportionalRefundForSupplier() {
        // Given
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(4000));

        BigDecimal refundAmount = BigDecimal.valueOf(3000);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // When
        List<CashTransactionDto> refunds = cashTransactionService.executeProportionalRefund(
                purchaseInvoice.getId(),
                refundAmount,
                "Test proportional refund",
                1L
        );

        // Then
        assertThat(refunds).hasSize(2);
        assertThat(refunds.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1800));
        assertThat(refunds.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1200));

        // Balance should increase (money comes back from supplier)
        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.add(refundAmount));

        // Check refund transaction types
        for (CashTransactionDto refund : refunds) {
            assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        }
    }

    // TEST-04: Execute proportional refund for customer (balance decreases)
    @Test
    @DisplayName("Should execute proportional refund for customer")
    void testExecuteProportionalRefundForCustomer() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(5000));

        createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(3000));
        createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(2000));

        BigDecimal refundAmount = BigDecimal.valueOf(2500);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // When
        List<CashTransactionDto> refunds = cashTransactionService.executeProportionalRefund(
                salesInvoice.getId(),
                refundAmount,
                "Test proportional refund",
                1L
        );

        // Then
        assertThat(refunds).hasSize(2);
        assertThat(refunds.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(refunds.get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        // Balance should decrease (money goes back to customer)
        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(refundAmount));
    }

    // =========================================================================
    // VALIDATION TESTS
    // =========================================================================

    // TEST-05: Refund amount exceeds paid amount
    @Test
    @DisplayName("Should throw exception when refund amount exceeds paid amount")
    void testRefundAmountExceedsPaidThrowsException() {
        // Given
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));

        BigDecimal refundAmount = BigDecimal.valueOf(7000);

        // When & Then
        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                purchaseInvoice.getId(), refundAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TEST-06: No payments found for invoice
    @Test
    @DisplayName("Should throw exception when no payments found for invoice")
    void testNoPaymentsThrowsException() {
        // Given
        Invoice invoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        // When & Then
        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                invoice.getId(), BigDecimal.valueOf(1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TEST-07: Zero refund amount
    @Test
    @DisplayName("Should throw exception when refund amount is zero")
    void testZeroRefundAmountThrowsException() {
        // Given
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));
        createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));

        // When & Then
        assertThatThrownBy(() -> cashTransactionService.executeProportionalRefund(
                purchaseInvoice.getId(), BigDecimal.ZERO, "Zero refund", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // TEST-08: Invoice not found
    @Test
    @DisplayName("Should throw exception when invoice not found")
    void testInvoiceNotFoundThrowsException() {
        // When & Then
        assertThatThrownBy(() -> cashTransactionService.calculateProportionalRefunds(
                99999L, BigDecimal.valueOf(1000)))
                .isInstanceOf(ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException.class);
    }

    // =========================================================================
    // SINGLE TRANSACTION REFUND TESTS (existing functionality)
    // =========================================================================

    // TEST-09: Create single partial refund
    @Test
    @DisplayName("Should create single partial refund correctly")
    void testCreatePartialRefund() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        BigDecimal refundAmount = BigDecimal.valueOf(300);
        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // When
        CashTransactionDto refund = cashTransactionService.createPartialRefund(
                payment.getId(), refundAmount, "Partial refund", 1L);

        // Then
        assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getAmount()).isEqualByComparingTo(refundAmount);

        // Balance should decrease by refund amount
        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(refundAmount));
    }

    // TEST-10: Full transaction refund (cancel)
    @Test
    @DisplayName("Should create full transaction refund correctly")
    void testCancelTransaction() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        BigDecimal balanceBefore = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // When
        CashTransactionDto refund = cashTransactionService.cancelTransaction(
                payment.getId(), "Full refund", 1L);

        // Then
        assertThat(refund.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        // Balance should return to original
        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());
        assertThat(balanceAfter).isEqualByComparingTo(balanceBefore.subtract(BigDecimal.valueOf(1000)));
    }

    // TEST-11: Double refund prevention
    @Test
    @DisplayName("Should throw exception when refunding already refunded transaction")
    void testDoubleRefundThrowsException() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        // First refund - should succeed
        cashTransactionService.cancelTransaction(payment.getId(), "First refund", 1L);

        // When & Then: Second refund should fail
        assertThatThrownBy(() -> cashTransactionService.cancelTransaction(payment.getId(), "Second refund", 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // INVOICE STATUS UPDATE TEST
    // =========================================================================

    // TEST-12: Invoice status becomes REFUNDED after full refund
    @Test
    @DisplayName("Should update invoice status after full refund")
    void testInvoiceStatusAfterFullRefund() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        // When
        cashTransactionService.cancelTransaction(payment.getId(), "Full refund", 1L);

        // Then
        Invoice updatedInvoice = invoiceRepository.findById(salesInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Invoice createTestInvoice(OrderType orderType, BigDecimal amount) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("TEST-INV-" + System.currentTimeMillis())
                .amount(amount)
                .currency("RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .issueDate(LocalDateTime.now())
                .build();

        if (orderType == OrderType.SALES) {
            invoice.setSalesOrderId(1L);
        } else {
            invoice.setPurchaseOrderId(1L);
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

        // Update invoice status after payment
        if (type == TransactionType.INCOME || type == TransactionType.EXPENSE) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
        }

        return saved;
    }
}