package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Soft Delete Fields Tests")
class SoftDeleteFieldsTest {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private Long testOrderId;
    private Long testInvoiceId;

    @BeforeEach
    void setUp() {
        log.info("=== SETUP: Creating test data ===");

        // Create sales order
        SalesOrder order = SalesOrder.builder()
                .orderNumber("TEST-SO-SOFT-" + System.currentTimeMillis())
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .subtotal(BigDecimal.valueOf(100))
                .createdBy(1L)
                .build();
        order = salesOrderRepository.save(order);
        testOrderId = order.getId();

        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber("TEST-INV-SOFT-" + System.currentTimeMillis())
                .salesOrderId(testOrderId)
                .amount(BigDecimal.valueOf(100))
                .currency("RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .issueDate(LocalDateTime.now())
                .createdBy(1L)
                .build();
        invoice = invoiceRepository.save(invoice);
        testInvoiceId = invoice.getId();

        log.info("=== SETUP COMPLETE: OrderId={}, InvoiceId={}", testOrderId, testInvoiceId);
    }

    @Test
    @DisplayName("TEST-01: New entities should have deleted=false by default")
    void testNewEntitiesHaveDeletedFalse() {
        log.info("=== TEST-01 START ===");

        SalesOrder order = salesOrderRepository.findById(testOrderId).orElse(null);
        assertThat(order).isNotNull();
        assertThat(order.isDeleted()).isFalse();
        assertThat(order.getDeletedAt()).isNull();
        assertThat(order.getDeletedBy()).isNull();
        assertThat(order.getDeleteReason()).isNull();

        Invoice invoice = invoiceRepository.findById(testInvoiceId).orElse(null);
        assertThat(invoice).isNotNull();
        assertThat(invoice.isDeleted()).isFalse();
        assertThat(invoice.getDeletedAt()).isNull();
        assertThat(invoice.getDeletedBy()).isNull();
        assertThat(invoice.getDeleteReason()).isNull();

        log.info("=== TEST-01 PASSED ===");
    }

    @Test
    @DisplayName("TEST-02: Soft delete fields can be updated")
    void testSoftDeleteFieldsCanBeUpdated() {
        log.info("=== TEST-02 START ===");

        SalesOrder order = salesOrderRepository.findById(testOrderId).orElse(null);
        assertThat(order).isNotNull();

        // Update soft delete fields
        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        order.setDeletedBy(99L);
        order.setDeleteReason("Test deletion");

        SalesOrder updated = salesOrderRepository.save(order);

        assertThat(updated.isDeleted()).isTrue();
        assertThat(updated.getDeletedAt()).isNotNull();
        assertThat(updated.getDeletedBy()).isEqualTo(99L);
        assertThat(updated.getDeleteReason()).isEqualTo("Test deletion");

        log.info("=== TEST-02 PASSED ===");
    }

    @Test
    @DisplayName("TEST-03: Soft deleted entities are still in database")
    void testSoftDeletedEntitiesStillExist() {
        log.info("=== TEST-03 START ===");

        // Soft delete
        SalesOrder order = salesOrderRepository.findById(testOrderId).orElse(null);
        Assertions.assertNotNull(order);
        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        order.setDeletedBy(1L);
        order.setDeleteReason("Soft delete test");
        salesOrderRepository.save(order);

        // Entity should still exist in database
        SalesOrder found = salesOrderRepository.findById(testOrderId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.isDeleted()).isTrue();

        log.info("=== TEST-03 PASSED ===");
    }
}