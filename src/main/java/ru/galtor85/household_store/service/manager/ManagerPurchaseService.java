package ru.galtor85.household_store.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.converter.PurchaseOrderConverter;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderDto;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.request.stock.StockWriteOffRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.dto.response.supplier.SupplierDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.mapper.supplier.SupplierMapper;
import ru.galtor85.household_store.processor.cell.CellBasedReceivingProcessor;
import ru.galtor85.household_store.processor.purchase.PurchaseOrderProcessor;
import ru.galtor85.household_store.processor.purchase.PurchaseOrderQueryProcessor;
import ru.galtor85.household_store.processor.purchase.PurchaseReceivingProcessor;
import ru.galtor85.household_store.processor.stock.StockWriteOffProcessor;
import ru.galtor85.household_store.processor.supplier.SupplierProductProcessor;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.date.DateParser;
import ru.galtor85.household_store.validator.order.PurchaseOrderValidator;
import ru.galtor85.household_store.validator.supplier.SupplierValidator;
import ru.galtor85.household_store.validator.common.ValidationHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPurchaseService {

    // Репозитории
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;

    // Валидаторы
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final SupplierValidator supplierValidator;
    private final ValidationHelper validationHelper;

    // Процессоры
    private final PurchaseOrderProcessor purchaseOrderProcessor;
    private final PurchaseReceivingProcessor purchaseReceivingProcessor;
    private final CellBasedReceivingProcessor cellBasedReceivingProcessor;
    private final StockWriteOffProcessor stockWriteOffProcessor;
    private final SupplierProductProcessor supplierProductProcessor;
    private final PurchaseOrderQueryProcessor purchaseOrderQueryProcessor;

    // Конвертеры
    private final PurchaseOrderConverter purchaseOrderConverter;

    // Мапперы
    private final SupplierMapper supplierMapper;

    // Утилиты
    private final MessageService messageService;
    private final DateParser dateParser;


    // =========================================================================
    // СОЗДАНИЕ ЗАКАЗА НА ЗАКУПКУ
    // =========================================================================

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId) {

        log.info(messageService.get("manager.purchase.create.start",
                request.getSupplierId(), managerId));

        // 1. Валидация
        purchaseOrderValidator.validateNotEmpty(request);
        Supplier supplier = purchaseOrderValidator.validateSupplierActive(request.getSupplierId());
        PurchaseOrderValidator.ProductValidationResult validationResult =
                purchaseOrderValidator.validateProducts(request.getItems());

        // 2. Создание заказа через процессор (вся бизнес-логика внутри)
        PurchaseOrder order = purchaseOrderProcessor.createPurchaseOrder(
                request,
                supplier,
                validationResult.getProducts(),
                validationResult.getPrices(),
                managerId
        );

        // 3. Конвертация в DTO
        PurchaseOrderDto result = purchaseOrderConverter.toDto(order, supplier.getName());

        log.info(messageService.get("manager.purchase.created.log",
                order.getOrderNumber(),
                request.getSupplierId(),
                managerId,
                order.getItems().size(),
                order.getTotalAmount()));

        return result;
    }

    // =========================================================================
    // ПРИЕМКА ЗАКАЗА (БЕЗ ЯЧЕЕК)
    // =========================================================================

    @Transactional
    public PurchaseOrderDto receivePurchaseOrder(Long orderId,
                                                 ReceiveAndStockRequest request,
                                                 Long managerId) {

        log.info(messageService.get("manager.purchase.receive.start", orderId, managerId));

        // 1. Валидация заказа
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        purchaseOrderValidator.validateOrderForReceiving(order);
        purchaseOrderValidator.validateWarehouse(request.getWarehouseId());

        // 2. Обработка приемки через процессор
        PurchaseReceivingProcessor.ReceivingResult result =
                purchaseReceivingProcessor.processReceiving(order, request, managerId);

        // 3. Обновление статуса заказа
        updateOrderStatusAfterReceiving(order, result, request, managerId);

        // 4. Сохранение
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(messageService.get("manager.purchase.received.log",
                orderId, managerId, result.getMovements().size(),
                result.isFullyReceived() ? "FULLY" : "PARTIALLY"));

        return purchaseOrderConverter.toDto(savedOrder,
                supplierRepository.findById(order.getSupplierId())
                        .map(Supplier::getName)
                        .orElse(null));
    }


    // =========================================================================
    // ПРИЕМКА ЗАКАЗА С РАЗМЕЩЕНИЕМ ПО ЯЧЕЙКАМ
    // =========================================================================

    @Transactional
    public PurchaseOrderDto receivePurchaseOrderWithStock(Long orderId,
                                                          ReceiveAndStockRequest request,
                                                          Long managerId) {

        log.info(messageService.get("manager.purchase.receive.with.stock.start",
                orderId, managerId, request.getWarehouseId()));

        // 1. Валидация заказа
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        purchaseOrderValidator.validateOrderForReceiving(order);
        purchaseOrderValidator.validateWarehouse(request.getWarehouseId());

        // 2. Обработка приемки с размещением по ячейкам
        CellBasedReceivingProcessor.CellBasedReceivingResult result =
                cellBasedReceivingProcessor.processReceivingWithCells(
                        order,
                        request.getItems(),
                        request.getWarehouseId(),
                        managerId);

        // 3. Обновление статуса заказа
        updateOrderStatusAfterCellBasedReceiving(order, result, request, managerId);

        // 4. Обновление информации о приемке в заказе
        updatePurchaseOrderReceivingInfo(order, request, managerId, result);

        // 5. Добавление заметки о приемке
        addCellBasedReceivingNote(order, request, result, managerId);

        // 6. Сохранение
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(messageService.get("manager.purchase.received.with.stock.log",
                orderId, managerId, result.getMovements().size(),
                result.getPlacements().size(), result.isFullyReceived()));

        return purchaseOrderConverter.toDto(savedOrder,
                supplierRepository.findById(order.getSupplierId())
                        .map(Supplier::getName)
                        .orElse(null));
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ СТАТУСА
    // =========================================================================

    /**
     * Обновляет статус заказа после приемки (без ячеек)
     */
    private void updateOrderStatusAfterReceiving(PurchaseOrder order,
                                                 PurchaseReceivingProcessor.ReceivingResult result,
                                                 ReceiveAndStockRequest request,
                                                 Long managerId) {

        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setActualDelivery(LocalDate.now());
            log.info(messageService.get("purchase.salesOrder.fully.received", order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(messageService.get("purchase.salesOrder.partially.received", order.getId()));

            if (!result.getUnreceivedItems().isEmpty()) {
                log.warn(messageService.get("purchase.salesOrder.unreceived.items",
                        result.getUnreceivedItems().size(), order.getId()));
            }
        }

        order.setReceivedBy(managerId);
        order.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }
    }

    /**
     * Обновляет статус заказа после приемки с ячейками
     */
    private void updateOrderStatusAfterCellBasedReceiving(PurchaseOrder order,
                                                          CellBasedReceivingProcessor.CellBasedReceivingResult result,
                                                          ReceiveAndStockRequest request,
                                                          Long managerId) {

        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setActualDelivery(LocalDate.now());
            log.info(messageService.get("purchase.salesOrder.fully.received", order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(messageService.get("purchase.salesOrder.partially.received", order.getId()));

            if (!result.getFailedItems().isEmpty()) {
                log.warn(messageService.get("purchase.salesOrder.failed.items",
                        result.getFailedItems().size(), order.getId()));
            }
        }

        order.setReceivedBy(managerId);
        order.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }
    }

    /**
     * Обновляет информацию о приемке в заказе
     */
    private void updatePurchaseOrderReceivingInfo(PurchaseOrder order,
                                                  ReceiveAndStockRequest request,
                                                  Long managerId,
                                                  CellBasedReceivingProcessor.CellBasedReceivingResult result) {
        order.setActualDelivery(LocalDate.now());
        order.setReceivedBy(managerId);
        order.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());
        }

        if (request.getWarehouseLocation() != null) {
            order.setWarehouseLocation(request.getWarehouseLocation());
        }
    }

    /**
     * Добавляет заметку о приемке с ячейками
     */
    private void addCellBasedReceivingNote(PurchaseOrder order,
                                           ReceiveAndStockRequest request,
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
            note.append(messageService.get("purchase.receiving.note.unreceived.count",
                    result.getFailedItems().size()));
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

    // =========================================================================
    // СПИСАНИЕ ТОВАРОВ
    // =========================================================================

    @Transactional
    public void writeOffStock(StockWriteOffRequest request, Long managerId) {
        stockWriteOffProcessor.processWriteOff(request, managerId);
    }

    // =========================================================================
    // УПРАВЛЕНИЕ ПОСТАВЩИКАМИ
    // =========================================================================

    @Transactional
    public SupplierDto createSupplier(SupplierCreateRequest request, Long managerId) {
        supplierValidator.validateUniqueness(request);

        Supplier supplier = supplierMapper.toEntity(request, managerId);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get("manager.supplier.created.log",
                savedSupplier.getName(), savedSupplier.getId(), managerId));

        return supplierMapper.toDto(savedSupplier);
    }

    @Transactional
    public SupplierDto updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier supplier = supplierValidator.validateExists(supplierId);
        supplierValidator.validateUniquenessOnUpdate(supplier, request);

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

    // =========================================================================
    // УПРАВЛЕНИЕ ТОВАРАМИ ПОСТАВЩИКОВ
    // =========================================================================

    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId,
                                                   Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId) {
        return supplierProductProcessor.addProductToSupplier(supplierId, productId, request, managerId);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long supplierProductId,
                                                    SupplierProductRequest request,
                                                    Long managerId) {
        return supplierProductProcessor.updateSupplierProduct(supplierProductId, request, managerId);
    }

    @Transactional
    public void removeProductFromSupplier(Long supplierProductId, Long managerId) {
        supplierProductProcessor.removeProductFromSupplier(supplierProductId, managerId);
    }

    // =========================================================================
    // ЗАПРОСЫ ЗАКУПОК
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getPurchaseOrders(Long supplierId,
                                                    String status,
                                                    String startDate,
                                                    String endDate,
                                                    int page,
                                                    int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime start = dateParser.parseDate(startDate);
        LocalDateTime end = dateParser.parseDate(endDate);
        validationHelper.validateDateRange(start, end, startDate, endDate);

        OrderStatus orderStatus = dateParser.parseOrderStatus(status);

        Page<PurchaseOrder> orders = purchaseOrderQueryProcessor.getPurchaseOrders(
                supplierId, orderStatus, start, end, pageable);

        return orders.map(order -> {
            String supplierName = supplierRepository.findById(order.getSupplierId())
                    .map(Supplier::getName)
                    .orElse(null);
            return purchaseOrderConverter.toDto(order, supplierName);
        });
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long orderId) {
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        String supplierName = supplierRepository.findById(order.getSupplierId())
                .map(Supplier::getName)
                .orElse(null);
        return purchaseOrderConverter.toDto(order, supplierName);
    }
}