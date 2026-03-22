package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.OrderBuilder;
import ru.galtor85.household_store.builder.PurchaseOrderBuilder;
import ru.galtor85.household_store.converter.OrderConverter;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.mapper.OrderMapper;
import ru.galtor85.household_store.mapper.SupplierMapper;
import ru.galtor85.household_store.processor.*;
import ru.galtor85.household_store.repository.*;
import ru.galtor85.household_store.util.*;
import ru.galtor85.household_store.validator.ValidationHelper;
import ru.galtor85.household_store.validator.WarehouseValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPurchaseService {

    private final OrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;
    private final CellBasedReceivingProcessor cellBasedReceivingProcessor;
    private final WarehouseValidator warehouseValidator;

    // Мапперы
    private final SupplierMapper supplierMapper;

    // Конвертеры
    private final OrderConverter orderConverter;

    // Билдеры
    private final OrderBuilder orderBuilder;
    private final PurchaseOrderBuilder purchaseOrderBuilder;

    // Утилиты
    private final EntityFinder entityFinder;
    private final ValidationHelper validationHelper;
    private final DateParser dateParser;

    // Процессоры
    private final PurchaseReceivingProcessor receivingProcessor;
    private final OrderStatusUpdateProcessor statusUpdateProcessor;
    private final WriteOffProcessor writeOffProcessor;
    private final SupplierProductProcessor supplierProductProcessor;
    private final PurchaseOrderQueryProcessor queryProcessor;

    // ========== PURCHASE ORDER CREATION ==========

    @Transactional
    public OrderDto createPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId) {
        entityFinder.findActiveSupplierById(request.getSupplierId());

        Order order = orderBuilder.buildPurchaseOrder(request, managerId);
        BigDecimal totalAmount = orderBuilder.calculateAndAddItems(order, request);
        order.setTotalAmount(totalAmount);
        order.setSubtotal(totalAmount);

        Order savedOrder = orderRepository.save(order);
        PurchaseOrder purchaseOrder = purchaseOrderBuilder.buildFromOrder(savedOrder, request);
        purchaseOrderRepository.save(purchaseOrder);

        log.info(messageService.get("manager.purchase.created.log",
                order.getOrderNumber(), request.getSupplierId(), managerId));

        return orderConverter.convertToDto(savedOrder);
    }

    // ========== RECEIVE PURCHASE ORDER ==========

    @Transactional
    public OrderDto receivePurchaseOrder(Long orderId, ReceiveAndStockRequest request, Long managerId) {
        // 1. Проверка
        Order order = entityFinder.findPurchaseOrderById(orderId);
        validationHelper.validateOrderForReceiving(order);


        // 2. Детали закупки
        PurchaseOrder purchaseOrder = entityFinder.findPurchaseOrderDetails(orderId);

        // 3. Обработка приемки
        PurchaseReceivingProcessor.ReceivingResult result = receivingProcessor.processReceiving(
                order, purchaseOrder, request, managerId);

        // 4. Обновление статуса
        statusUpdateProcessor.updateAfterReceiving(order, purchaseOrder, result, request, managerId);

        // 5. Сохранение
        orderRepository.save(order);
        purchaseOrderRepository.save(purchaseOrder);

        log.info(messageService.get("manager.purchase.received.log",
                orderId, managerId, result.getMovements().size()));

        return orderConverter.convertToDto(order);
    }

    @Transactional
    public OrderDto receivePurchaseOrderWithStock(Long orderId, ReceiveAndStockRequest request, Long managerId) {
        // 1. Проверка заказа
        Order order = entityFinder.findPurchaseOrderById(orderId);
        validationHelper.validateOrderForReceiving(order);

        // 2. Детали закупки
        PurchaseOrder purchaseOrder = entityFinder.findPurchaseOrderDetails(orderId);

        // 3. Обработка приемки с размещением по ячейкам
        CellBasedReceivingProcessor.CellBasedReceivingResult result =
                cellBasedReceivingProcessor.processReceivingWithCells(
                        order, request.getItems(), request.getWarehouseId(), managerId);

        // 4. Обновление статуса заказа
        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            log.info(messageService.get("purchase.order.fully.received", order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(messageService.get("purchase.order.partially.received", order.getId()));

            if (!result.getFailedItems().isEmpty()) {
                log.warn(messageService.get("purchase.order.failed.items",
                        result.getFailedItems().size(), order.getId()));
            }
        }

        // 5. Установка даты приемки
        order.setDeliveredAt(LocalDateTime.now());

        // 6. Обновление информации в purchaseOrder
        purchaseOrder.setActualDelivery(LocalDate.now());
        purchaseOrder.setReceivedBy(managerId);
        purchaseOrder.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            purchaseOrder.setPaymentStatus(request.getPaymentStatus());
        }

        // 7. Добавление заметки о приемке
        addCellBasedReceivingNote(order, request, result, managerId);

        // 8. Сохранение
        orderRepository.save(order);
        purchaseOrderRepository.save(purchaseOrder);

        log.info(messageService.get("manager.purchase.received.with.stock.log",
                orderId, managerId, result.getMovements().size(), result.getPlacements().size()));

        return orderConverter.convertToDto(order);
    }

    private void addCellBasedReceivingNote(Order order, ReceiveAndStockRequest request,
                                           CellBasedReceivingProcessor.CellBasedReceivingResult result,
                                           Long managerId) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        StringBuilder note = new StringBuilder();

        note.append(messageService.get("purchase.receiving.note.header",
                timestamp, managerId));

        if (result.isFullyReceived()) {
            note.append(messageService.get("purchase.receiving.note.fully.received"));
        } else {
            note.append(messageService.get("purchase.receiving.note.partially.received"));
        }

        note.append(messageService.get("purchase.receiving.note.cells.used",
                result.getPlacements().size()));

        if (request.getQualityCheck() != null) {
            note.append(request.getQualityCheck() ?
                    messageService.get("purchase.receiving.note.quality.passed") :
                    messageService.get("purchase.receiving.note.quality.failed"));
        }

        if (!result.getFailedItems().isEmpty()) {
            note.append(messageService.get("purchase.receiving.note.failed.items",
                    result.getFailedItems().size()));
        }

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            note.append(messageService.get("purchase.receiving.note.notes",
                    request.getNotes()));
        }

        String currentNotes = order.getNotes();
        if (currentNotes == null || currentNotes.isEmpty()) {
            order.setNotes(note.toString());
        } else {
            order.setNotes(currentNotes + "\n" + note.toString());
        }
    }

    // ========== STOCK WRITE-OFF ==========

    @Transactional
    public void writeOffStock(StockWriteOffRequest request, Long managerId) {
        writeOffProcessor.processWriteOff(request, managerId);
    }

    // ========== SUPPLIER MANAGEMENT ==========

    @Transactional
    public SupplierDto createSupplier(SupplierCreateRequest request, Long managerId) {
        validationHelper.validateSupplierUniqueness(request);

        Supplier supplier = supplierMapper.toEntity(request, managerId);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get("manager.supplier.created.log",
                savedSupplier.getName(), savedSupplier.getId(), managerId));

        return supplierMapper.toDto(savedSupplier);
    }

    @Transactional
    public SupplierDto updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier supplier = entityFinder.findSupplierById(supplierId);
        validationHelper.validateSupplierUniquenessOnUpdate(supplier, request);

        supplierMapper.updateEntity(supplier, request);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get("manager.supplier.updated.log", updatedSupplier.getId()));

        return supplierMapper.toDto(updatedSupplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(String name, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Supplier> suppliers = supplierRepository.searchSuppliersNative(name, status, pageable);

        log.debug(messageService.get("manager.suppliers.fetched.log", suppliers.getTotalElements()));

        return suppliers.map(supplierMapper::toDto);
    }

    // ========== SUPPLIER PRODUCT MANAGEMENT ==========

    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId, Long productId,
                                                   SupplierProductRequest request, Long managerId) {
        return supplierProductProcessor.addProductToSupplier(supplierId, productId, request, managerId);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long supplierProductId,
                                                    SupplierProductRequest request, Long managerId) {
        return supplierProductProcessor.updateSupplierProduct(supplierProductId, request, managerId);
    }

    @Transactional
    public void removeProductFromSupplier(Long supplierProductId, Long managerId) {
        supplierProductProcessor.removeProductFromSupplier(supplierProductId, managerId);
    }

    // ========== PURCHASE ORDER QUERIES ==========

    @Transactional(readOnly = true)
    public Page<OrderDto> getPurchaseOrders(Long supplierId, String status,
                                            String startDate, String endDate,
                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime start = dateParser.parseDate(startDate);
        LocalDateTime end = dateParser.parseDate(endDate);

        validationHelper.validateDateRange(start, end, startDate, endDate);

        OrderStatus orderStatus = dateParser.parseOrderStatus(status);
        return queryProcessor.getPurchaseOrders(supplierId, orderStatus, start, end, pageable);
    }

    @Transactional(readOnly = true)
    public OrderDto getPurchaseOrderById(Long orderId) {
        Order order = entityFinder.findPurchaseOrderById(orderId);
        return orderConverter.convertToDto(order);
    }
}