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
import java.math.RoundingMode;
import java.time.LocalDateTime;

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

    @Test
    @DisplayName("Should decrease balance when refunding sales order (customer refund)")
    void testSalesOrderRefundDecreasesBalance() {
        // Given
        Invoice salesInvoice = createTestInvoice(OrderType.SALES, BigDecimal.valueOf(1000));
        CashTransaction payment = createTestTransaction(cashRegister, salesInvoice,
                TransactionType.INCOME, BigDecimal.valueOf(1000));

        // When
        CashTransactionDto refundDto = cashTransactionService.cancelTransaction(
                payment.getId(), "Test refund", 1L);

        // Then
        assertThat(refundDto.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refundDto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(refundDto.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("Should increase balance when refunding purchase order (supplier refund)")
    void testPurchaseOrderRefundIncreasesBalance() {
        // Given
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(500));
        CashTransaction payment = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(500));

        // When
        CashTransactionDto refundDto = cashTransactionService.cancelTransaction(
                payment.getId(), "Supplier refund", 1L);

        // Then
        assertThat(refundDto.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refundDto.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(refundDto.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

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

    @Test
    @DisplayName("Should update invoice status to REFUNDED after full refund")
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
        assertThat(updatedInvoice.getTotalPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Invoice createTestInvoice(OrderType orderType, BigDecimal amount) {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("TEST-INV-" + System.currentTimeMillis())
                .amount(amount)
                .currency("RUB")
                .status(InvoiceStatus.PAID)
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
        BigDecimal balanceAfter = type == TransactionType.INCOME
                ? balanceBefore.add(amount)
                : balanceBefore.subtract(amount);

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

        return cashTransactionRepository.save(transaction);
    }

    @Test
    @DisplayName("Should create proportional partial refund for supplier")
    void testProportionalPartialRefundForSupplier() {
        // Given: Purchase order with multiple payments
        Invoice purchaseInvoice = createTestInvoice(OrderType.PURCHASE, BigDecimal.valueOf(10000));

        // Create two partial payments
        CashTransaction payment1 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(6000));
        CashTransaction payment2 = createTestTransaction(cashRegister, purchaseInvoice,
                TransactionType.EXPENSE, BigDecimal.valueOf(4000));

        // When: Return goods worth 3000 (30% of total)
        BigDecimal refundAmount = BigDecimal.valueOf(3000);

        // Calculate proportional refunds
        BigDecimal proportionalRefund1 = payment1.getAmount()
                .multiply(refundAmount)
                .divide(purchaseInvoice.getAmount(), 2, RoundingMode.HALF_UP);
        BigDecimal proportionalRefund2 = payment2.getAmount()
                .multiply(refundAmount)
                .divide(purchaseInvoice.getAmount(), 2, RoundingMode.HALF_UP);

        // Create partial refunds
        CashTransactionDto refund1 = cashTransactionService.createPartialRefund(
                payment1.getId(), proportionalRefund1, "Partial return", 1L);
        CashTransactionDto refund2 = cashTransactionService.createPartialRefund(
                payment2.getId(), proportionalRefund2, "Partial return", 1L);

        // Then
        BigDecimal balanceAfter = cashRegisterService.getCurrentBalance(cashRegister.getId());

        // Balance should increase by total refund amount (money comes back)
        assertThat(balanceAfter).isEqualByComparingTo(refundAmount);

        // Check refund amounts
        assertThat(refund1.getAmount()).isEqualByComparingTo(proportionalRefund1);
        assertThat(refund2.getAmount()).isEqualByComparingTo(proportionalRefund2);

        // Check transaction types
        assertThat(refund1.getTransactionType()).isEqualTo(TransactionType.REFUND);
        assertThat(refund2.getTransactionType()).isEqualTo(TransactionType.REFUND);

        // Check invoice status (should be PARTIALLY_PAID or REFUNDED)
        Invoice updatedInvoice = invoiceRepository.findById(purchaseInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isIn(InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.REFUNDED);
    }
}