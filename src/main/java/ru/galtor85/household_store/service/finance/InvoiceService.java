package ru.galtor85.household_store.service.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.cash.InvoiceNotFoundException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderNotFoundException;
import ru.galtor85.household_store.advice.exception.order.SalesOrderNotFoundException;
import ru.galtor85.household_store.converter.InvoiceConverter;
import ru.galtor85.household_store.dto.request.finance.InvoiceCreateRequest;
import ru.galtor85.household_store.dto.response.finance.InvoiceDto;
import ru.galtor85.household_store.entity.finance.Invoice;
import ru.galtor85.household_store.entity.finance.InvoiceStatus;
import ru.galtor85.household_store.entity.finance.PaymentMethod;
import ru.galtor85.household_store.processor.invoice.InvoicePaymentProcessor;
import ru.galtor85.household_store.repository.finance.InvoiceRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.generator.NumberGenerator;
import ru.galtor85.household_store.validator.finance.InvoiceValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceConverter invoiceConverter;
    private final InvoiceValidator invoiceValidator;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;
    private final InvoicePaymentProcessor paymentProcessor;

    // =========================================================================
    // СОЗДАНИЕ СЧЕТА
    // =========================================================================

    /**
     * Создает новый счет
     */
    @Transactional
    public InvoiceDto createInvoice(InvoiceCreateRequest request, Long createdBy) {
        log.info(messageService.get("invoice.create.start",
                request.getPurchaseOrderId(), request.getSalesOrderId()));

        // Валидация
        invoiceValidator.validateCreateRequest(request);

        // Проверяем существование заказа
        if (request.getPurchaseOrderId() != null) {
            validatePurchaseOrder(request.getPurchaseOrderId());
        } else {
            validateSalesOrder(request.getSalesOrderId());
        }

        // Создаем счет
        Invoice invoice = Invoice.builder()
                .invoiceNumber(numberGenerator.generateInvoiceNumber())
                .purchaseOrderId(request.getPurchaseOrderId())
                .salesOrderId(request.getSalesOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "RUB")
                .status(InvoiceStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .issueDate(LocalDateTime.now())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.created", saved.getInvoiceNumber()));

        return invoiceConverter.toDto(saved);
    }

    // =========================================================================
    // ПОЛУЧЕНИЕ СЧЕТОВ
    // =========================================================================

    /**
     * Получает счет по ID
     */
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceValidator.validateInvoiceExists(invoiceId);
        return invoiceConverter.toDto(invoice);
    }

    /**
     * Получает счет по номеру
     */
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
        return invoiceConverter.toDto(invoice);
    }

    /**
     * Получает все счета с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDto> getAllInvoices(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return invoiceRepository.findAll(pageable)
                .map(invoiceConverter::toDto);
    }

    /**
     * Получает счета по статусу
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDto> getInvoicesByStatus(InvoiceStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("issueDate").descending());
        return invoiceRepository.findByStatus(status, pageable)
                .map(invoiceConverter::toDto);
    }

    /**
     * Получает счета для заказа на закупку
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesByPurchaseOrder(Long purchaseOrderId) {
        validatePurchaseOrder(purchaseOrderId);
        return invoiceRepository.findByPurchaseOrderId(purchaseOrderId)
                .stream()
                .map(invoiceConverter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает счета для заказа на продажу
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesBySalesOrder(Long salesOrderId) {
        validateSalesOrder(salesOrderId);
        return invoiceRepository.findBySalesOrderId(salesOrderId)
                .stream()
                .map(invoiceConverter::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ОПЛАТА СЧЕТОВ
    // =========================================================================

    /**
     * Отмечает счет как оплаченный
     */
    @Transactional
    public InvoiceDto markAsPaid(Long invoiceId, Long cashRegisterId, Long cashierId) {

        InvoicePaymentProcessor.InvoicePaymentResult result = paymentProcessor.processPayment(
                invoiceId,
                getInvoiceAmount(invoiceId),
                cashRegisterId,
                cashierId,
                false
        );

        log.info(messageService.get("invoice.paid", result.getInvoice().getInvoiceNumber()));

        return invoiceConverter.toDto(result.getInvoice());
    }

    /**
     * Частичная оплата счета
     */
    @Transactional
    public InvoiceDto markAsPartiallyPaid(Long invoiceId, BigDecimal paidAmount,Long cashRegisterId, Long cashierId) {
        InvoicePaymentProcessor.InvoicePaymentResult result = paymentProcessor.processPayment(
                invoiceId, paidAmount, cashRegisterId, cashierId, true
        );

        log.info(messageService.get("invoice.partially.paid",
                result.getInvoice().getInvoiceNumber(), paidAmount));

        return invoiceConverter.toDto(result.getInvoice());
    }

    // =========================================================================
    // ОТМЕНА СЧЕТОВ
    // =========================================================================

    /**
     * Отменяет счет
     */
    @Transactional
    public InvoiceDto cancelInvoice(Long invoiceId, String reason, Long cancelledBy) {
        log.info(messageService.get("invoice.cancel.start", invoiceId, reason));

        Invoice invoice = invoiceValidator.validateInvoiceExists(invoiceId);
        invoiceValidator.validateInvoiceCancellable(invoice);

        invoice.setStatus(InvoiceStatus.CANCELLED);
        String cancelNote = String.format("Отменен: %s (пользователь %d)", reason, cancelledBy);
        if (invoice.getNotes() == null) {
            invoice.setNotes(cancelNote);
        } else {
            invoice.setNotes(invoice.getNotes() + "\n" + cancelNote);
        }

        Invoice saved = invoiceRepository.save(invoice);

        log.info(messageService.get("invoice.cancelled", saved.getInvoiceNumber()));

        return invoiceConverter.toDto(saved);
    }

    // =========================================================================
    // СТАТИСТИКА
    // =========================================================================

    /**
     * Получает общую сумму ожидающих оплаты счетов
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingAmount() {
        return invoiceRepository.getTotalPendingAmount();
    }

    /**
     * Получает сумму оплаченных счетов за период
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmountForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.getTotalPaidAmountForPeriod(startDate, endDate);
    }

    /**
     * Получает статистику по статусам счетов
     */
    @Transactional(readOnly = true)
    public Map<InvoiceStatus, BigDecimal> getTotalAmountByStatus() {
        List<Object[]> results = invoiceRepository.getTotalAmountByStatus();
        Map<InvoiceStatus, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            InvoiceStatus status = (InvoiceStatus) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(status, amount);
        }
        return map;
    }

    /**
     * Получает статистику по способам оплаты
     */
    @Transactional(readOnly = true)
    public Map<PaymentMethod, BigDecimal> getTotalAmountByPaymentMethod() {
        List<Object[]> results = invoiceRepository.getTotalAmountByPaymentMethod();
        Map<PaymentMethod, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(method, amount);
        }
        return map;
    }

    // =========================================================================
    // ПРОСРОЧЕННЫЕ СЧЕТА
    // =========================================================================

    /**
     * Получает просроченные счета
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now())
                .stream()
                .map(invoiceConverter::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает счета со скорым сроком оплаты (следующие N дней)
     */
    @Transactional(readOnly = true)
    public List<InvoiceDto> getUpcomingInvoices(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(days);
        return invoiceRepository.findUpcomingInvoices(now, endDate)
                .stream()
                .map(invoiceConverter::toDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Получает сумму счёта по ID
     */
    private BigDecimal getInvoiceAmount(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(Invoice::getAmount)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    private void validatePurchaseOrder(Long purchaseOrderId) {
        if (!purchaseOrderRepository.existsById(purchaseOrderId)) {
            throw new PurchaseOrderNotFoundException(purchaseOrderId);
        }
    }

    private void validateSalesOrder(Long salesOrderId) {
        if (!salesOrderRepository.existsById(salesOrderId)) {
            throw new SalesOrderNotFoundException(salesOrderId);
        }
    }
}