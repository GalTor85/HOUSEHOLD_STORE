package ru.galtor85.household_store.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderReverseException;
import ru.galtor85.household_store.converter.PurchaseOrderConverter;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.request.order.ReceiveAndStockRequest;
import ru.galtor85.household_store.dto.request.order.ReverseReceiptRequest;
import ru.galtor85.household_store.dto.request.stock.StockWriteOffRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierCreateRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.dto.request.supplier.SupplierUpdateRequest;
import ru.galtor85.household_store.dto.response.order.PurchaseOrderDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierDto;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.order.OrderPaymentStatus;
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
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.date.DateParser;
import ru.galtor85.household_store.validator.common.ValidationHelper;
import ru.galtor85.household_store.validator.order.PurchaseOrderValidator;
import ru.galtor85.household_store.validator.supplier.SupplierValidator;
import ru.galtor85.household_store.validator.warehouse.WarehouseValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing purchase orders and suppliers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPurchaseService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final SupplierValidator supplierValidator;
    private final ValidationHelper validationHelper;
    private final WarehouseValidator warehouseValidator;
    private final PurchaseOrderProcessor purchaseOrderProcessor;
    private final PurchaseReceivingProcessor purchaseReceivingProcessor;
    private final CellBasedReceivingProcessor cellBasedReceivingProcessor;
    private final StockWriteOffProcessor stockWriteOffProcessor;
    private final SupplierProductProcessor supplierProductProcessor;
    private final PurchaseOrderQueryProcessor purchaseOrderQueryProcessor;
    private final PurchaseOrderConverter purchaseOrderConverter;
    private final SupplierMapper supplierMapper;
    private final MessageService messageService;
    private final DateParser dateParser;
    private final LogMessageService logMsg;

    private static final String NOTES_SEPARATOR = " | ";
    private static final String NOTES_HEADER = "-- ";

// =========================================================================
    // PURCHASE ORDER CREATION
    // =========================================================================

    /**
     * Creates a new purchase order
     *
     * @param request   purchase order creation request
     * @param managerId ID of the manager creating the order
     * @return created purchase order DTO
     */
    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId) {

        log.info(logMsg.get("manager.purchase.create.start",
                request.getSupplierId(), managerId));

        // Validate request
        purchaseOrderValidator.validateNotEmpty(request);
        Supplier supplier = purchaseOrderValidator.validateSupplierActive(request.getSupplierId());
        PurchaseOrderValidator.ProductValidationResult validationResult =
                purchaseOrderValidator.validateProducts(request.getItems());

        // Create order via processor
        PurchaseOrder order = purchaseOrderProcessor.createPurchaseOrder(
                request,
                supplier,
                validationResult.products(),
                validationResult.prices(),
                managerId
        );

        // Convert to DTO
        PurchaseOrderDto result = purchaseOrderConverter.toDto(order, supplier.getName());

        log.info(logMsg.get("manager.purchase.created.log",
                order.getOrderNumber(),
                request.getSupplierId(),
                managerId,
                order.getItems().size(),
                order.getTotalAmount()));

        return result;
    }

    // =========================================================================
    // PURCHASE ORDER CANCELLATION
    // =========================================================================

    /**
     * Cancels a purchase order
     *
     * @param orderId   purchase order identifier
     * @param reason    cancellation reason
     * @param managerId ID of manager cancelling the order
     * @return cancelled purchase order DTO
     */
    @Transactional
    public PurchaseOrderDto cancelPurchaseOrder(Long orderId, String reason, Long managerId) {
        log.info(logMsg.get("manager.purchase.cancel.start", orderId, reason, managerId));

        // 1. Validate order exists
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);

        // 2. Check if order can be cancelled
        purchaseOrderValidator.validateOrderCancellable(order);

        // 3. Cancel order via processor
        PurchaseOrder cancelledOrder = purchaseOrderProcessor.cancelOrder(order, reason, managerId);

        // 4. Convert to DTO
        String supplierName = supplierRepository.findById(cancelledOrder.getSupplierId())
                .map(Supplier::getName)
                .orElse(null);

        PurchaseOrderDto result = purchaseOrderConverter.toDto(cancelledOrder, supplierName);

        log.info(logMsg.get("manager.purchase.cancel.complete", orderId, managerId));

        return result;
    }

    // =========================================================================
    // PURCHASE ORDER RECEIVING (WITHOUT CELLS)
    // =========================================================================

    /**
     * Receives a purchase order without cell placement
     *
     * @param orderId   purchase order identifier
     * @param request   receiving request
     * @param managerId ID of the manager processing the receipt
     * @return updated purchase order DTO
     */
    @Transactional
    public PurchaseOrderDto receivePurchaseOrder(Long orderId,
                                                 ReceiveAndStockRequest request,
                                                 Long managerId) {

        log.info(logMsg.get("manager.purchase.receive.start", orderId, managerId));



        // Validate order
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        purchaseOrderValidator.validateOrderForReceiving(order);
        order.setWarehouseLocation(warehouseValidator.validateWarehouseExists(request.getWarehouseId()).getName());


        // Process receiving
        PurchaseReceivingProcessor.ReceivingResult result =
                purchaseReceivingProcessor.processReceiving(order, request, managerId);

        // Update order status
        updateOrderStatusAfterReceiving(order, result, request, managerId);

        // Save order
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        log.info(logMsg.get("manager.purchase.received.log",
                orderId, managerId, result.getMovements().size(),
                result.isFullyReceived() ? "FULLY" : "PARTIALLY"));

        return purchaseOrderConverter.toDto(savedOrder,
                supplierRepository.findById(order.getSupplierId())
                        .map(Supplier::getName)
                        .orElse(null));
    }


    // =========================================================================
    // PURCHASE ORDER RECEIVING (WITH CELLS)
    // =========================================================================

    /**
     * Receives a purchase order with automatic cell placement
     *
     * @param orderId   purchase order identifier
     * @param request   receiving request with cell information
     * @param managerId ID of the manager processing the receipt
     * @return updated purchase order DTO
     */
    @Transactional
    public PurchaseOrderDto receivePurchaseOrderWithStock(Long orderId,
                                                          ReceiveAndStockRequest request,
                                                          Long managerId) {

        log.info(logMsg.get("manager.purchase.receive.with.stock.start",
                orderId, managerId, request.getWarehouseId()));

        // Validate order
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        purchaseOrderValidator.validateOrderForReceiving(order);
        warehouseValidator.validateWarehouseExists(request.getWarehouseId());

        // Process receiving with cell placement
        CellBasedReceivingProcessor.CellBasedReceivingResult result =
                cellBasedReceivingProcessor.processReceivingWithCells(
                        order,
                        request.getItems(),
                        request.getWarehouseId(),
                        managerId);

        // Update order status
        updateOrderStatusAfterCellBasedReceiving(order, result, request, managerId);

        // Update receiving information
        updatePurchaseOrderReceivingInfo(order, request, managerId);

        // Add receiving note
        addCellBasedReceivingNote(order, request, result, managerId);

        // Save order
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        PurchaseOrderDto orderDto = purchaseOrderConverter.toDto(savedOrder,
                supplierRepository.findById(order.getSupplierId())
                        .map(Supplier::getName)
                        .orElse(null));
        orderDto.setFailedItems(result.getFailedItems());
        orderDto.setErrorMessages(result.getErrorMessages());

        if (!result.isAllSuccess()) {
            log.warn(logMsg.get("purchase.order.partially.received.warn",
                    orderId, result.getFailedItems().size(), result.getFailedItems()));
        }

        log.info(logMsg.get("manager.purchase.received.with.stock.log",
                orderId, managerId, result.getMovements().size(),
                result.getPlacements().size(), result.isFullyReceived()));

        return orderDto;
    }

    // =========================================================================
    // REVERSE RECEIPT (RETURN TO SUPPLIER)
    // =========================================================================

    /**
     * Reverses a purchase order receipt (returns goods to supplier)
     *
     * @param request   reverse receipt request with reason and items
     * @param managerId ID of manager performing the reversal
     * @return updated purchase order DTO
     */
    @Transactional
    public PurchaseOrderDto reverseReceipt(ReverseReceiptRequest request, Long managerId) {
        log.info(logMsg.get("manager.purchase.reverse.start",
                request.getOrderId(), request.getReason(), managerId));

        // Validate order exists
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(request.getOrderId());

        // Validate order can be reversed
        validateOrderCanBeReversed(order);

        // Process reversal
        PurchaseReceivingProcessor.ReverseReceiptResult result =
                purchaseReceivingProcessor.reverseReceipt(order, request, managerId);

        // Update order status if all items reversed
        if (result.isAllReversed()) {
            order.setStatus(OrderStatus.PENDING);
            order.setReceivedBy(null);
            order.setActualDelivery(null);
            order.setQualityCheck(null);
            order.setWarehouseLocation(null);

            log.info(logMsg.get("manager.purchase.reverse.all.items", order.getId()));
        } else if (result.isAllSuccess() && !result.getReversedItems().isEmpty()) {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(logMsg.get("manager.purchase.reverse.partial", order.getId()));
        }

        // Add note about reversal
        addReverseReceiptNote(order, request, result, managerId);

        // Save order
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        // Convert to DTO
        String supplierName = supplierRepository.findById(savedOrder.getSupplierId())
                .map(Supplier::getName)
                .orElse(null);

        log.info(logMsg.get("manager.purchase.reverse.complete",
                request.getOrderId(), result.getReversedItems().size(), managerId));

        return purchaseOrderConverter.toDto(savedOrder, supplierName);
    }

    /**
     * Validates that an order can be reversed (has received items and not cancelled)
     *
     * @param order purchase order to validate
     * @throws PurchaseOrderReverseException if order cannot be reversed
     */

    private void validateOrderCanBeReversed(PurchaseOrder order) {
        boolean hasReceivedItems = order.getItems().stream()
                .anyMatch(item -> item.getReceivedQuantity() != null && item.getReceivedQuantity() > 0);

        if (!hasReceivedItems) {
            throw new PurchaseOrderReverseException(
                    messageService.get("purchase.order.cannot.reverse.no.items", order.getId())
            );
        }

        // Cannot reverse if already cancelled
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new PurchaseOrderReverseException(
                    messageService.get("purchase.order.cannot.reverse.cancelled", order.getId())
            );
        }
    }

    /**
     * Adds a note about reversal to the purchase order
     */
    private void addReverseReceiptNote(PurchaseOrder order,
                                       ReverseReceiptRequest request,
                                       PurchaseReceivingProcessor.ReverseReceiptResult result,
                                       Long managerId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        StringBuilder note = new StringBuilder();

        note.append(NOTES_HEADER).append(messageService.get("purchase.reverse.note.header", timestamp, managerId)).append(" --");
        note.append(NOTES_HEADER).append(messageService.get("purchase.reverse.note.reason", request.getReason()));

        if (request.getComments() != null && !request.getComments().isEmpty()) {
            note.append(NOTES_HEADER).append(messageService.get("purchase.reverse.note.comments", request.getComments())).append(" --");
        }

        note.append((NOTES_HEADER)).append(messageService.get("purchase.reverse.note.items", result.getReversedItems().size())).append(" --");

        if (!result.isAllSuccess()) {
            note.append((NOTES_HEADER)).append(messageService.get("purchase.reverse.note.failed", result.getFailedItems().size())).append(" --");
        }

        String currentNotes = order.getNotes();
        if (currentNotes == null || currentNotes.isEmpty()) {
            order.setNotes(note.toString());
        } else {
            order.setNotes(currentNotes +NOTES_SEPARATOR + note);
        }
    }

    // =========================================================================
    // HELPER METHODS FOR STATUS UPDATES
    // =========================================================================

    /**
     * Updates order status after receiving (without cells)
     */
    private void updateOrderStatusAfterReceiving(PurchaseOrder order,
                                                 PurchaseReceivingProcessor.ReceivingResult result,
                                                 ReceiveAndStockRequest request,
                                                 Long managerId) {

        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setActualDelivery(LocalDate.now());
            log.info(logMsg.get("purchase.salesOrder.fully.received", order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(logMsg.get("purchase.salesOrder.partially.received", order.getId()));

            if (!result.getUnreceivedItems().isEmpty()) {
                log.warn(logMsg.get("purchase.salesOrder.unreceived.items",
                        result.getUnreceivedItems().size(), order.getId()));
            }
        }

        setOrderFields(order, request, managerId);
    }

    /**
     * Updates order status after receiving with cells
     */
    private void updateOrderStatusAfterCellBasedReceiving(PurchaseOrder order,
                                                          CellBasedReceivingProcessor.CellBasedReceivingResult result,
                                                          ReceiveAndStockRequest request,
                                                          Long managerId) {

        if (result.isFullyReceived()) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setActualDelivery(LocalDate.now());
            log.info(logMsg.get("purchase.salesOrder.fully.received", order.getId()));
        } else {
            order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
            log.info(logMsg.get("purchase.salesOrder.partially.received", order.getId()));

            if (!result.getFailedItems().isEmpty()) {
                log.warn(logMsg.get("purchase.salesOrder.failed.items",
                        result.getFailedItems().size(), order.getId()));
            }
        }

        setOrderFields(order, request, managerId);
    }

    /**
     * Updates receiving information in the purchase order
     */
    private void updatePurchaseOrderReceivingInfo(PurchaseOrder order,
                                                  ReceiveAndStockRequest request,
                                                  Long managerId) {
        order.setActualDelivery(LocalDate.now());
        setOrderFields(order, request, managerId);

        if (request.getWarehouseLocation() != null) {
            order.setWarehouseLocation(request.getWarehouseLocation());
        }
    }

    private void setOrderFields(PurchaseOrder order, ReceiveAndStockRequest request, Long managerId) {
        order.setReceivedBy(managerId);
        order.setQualityCheck(request.getQualityCheck());

        if (request.getPaymentStatus() != null) {
            try {
                order.setPaymentStatus(OrderPaymentStatus.valueOf(request.getPaymentStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn(logMsg.get("order.payment.status.invalid", request.getPaymentStatus()));
            }
        }
    }

    /**
     * Adds a note about cell-based receiving to the purchase order
     */
    private void addCellBasedReceivingNote(PurchaseOrder order,
                                           ReceiveAndStockRequest request,
                                           CellBasedReceivingProcessor.CellBasedReceivingResult result,
                                           Long managerId) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        StringBuilder note = new StringBuilder();

        note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.header",
                timestamp, managerId));

        if (result.isFullyReceived()) {
            note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.fully.received"));
        } else {
            note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.partially.received"));
            note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.unreceived.count",
                    result.getFailedItems().size()));
        }

        note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.cells.used",
                result.getPlacements().size()));

        if (request.getQualityCheck() != null) {
            note.append(NOTES_HEADER).append(request.getQualityCheck() ?
                    messageService.get("purchase.receiving.note.quality.passed") :
                    messageService.get("purchase.receiving.note.quality.failed"));
        }

        if (!result.getFailedItems().isEmpty()) {
            note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.failed.items",
                    result.getFailedItems().size()));
        }

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            note.append(NOTES_HEADER).append(messageService.get("purchase.receiving.note.notes",
                    request.getNotes()));
        }

        String currentNotes = order.getNotes();
        if (currentNotes == null || currentNotes.isEmpty()) {
            order.setNotes(note.toString());
        } else {
            order.setNotes(currentNotes +NOTES_SEPARATOR + note);
        }
    }

    // =========================================================================
    // STOCK WRITE-OFF
    // =========================================================================

    /**
     * Writes off damaged or lost stock
     *
     * @param request   write-off request with items and reason
     * @param managerId ID of the manager processing the write-off
     */
    @Transactional
    public void writeOffStock(StockWriteOffRequest request, Long managerId) {
        stockWriteOffProcessor.processWriteOff(request, managerId);
    }

    // =========================================================================
    // SUPPLIER MANAGEMENT
    // =========================================================================

    /**
     * Creates a new supplier
     *
     * @param request   supplier creation request
     * @param managerId ID of the manager creating the supplier
     * @return created supplier DTO
     */
    @Transactional
    public SupplierDto createSupplier(SupplierCreateRequest request, Long managerId) {
        supplierValidator.validateUniqueness(request);

        Supplier supplier = supplierMapper.toEntity(request, managerId);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info(logMsg.get("manager.supplier.created.log",
                savedSupplier.getName(), savedSupplier.getId(), managerId));

        return supplierMapper.toDto(savedSupplier);
    }

    /**
     * Updates an existing supplier
     *
     * @param supplierId supplier identifier
     * @param request    supplier update request
     * @return updated supplier DTO
     */
    @Transactional
    public SupplierDto updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier supplier = supplierValidator.validateExists(supplierId);
        supplierValidator.validateUniquenessOnUpdate(supplier, request);

        supplierMapper.updateEntity(supplier, request);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info(logMsg.get("manager.supplier.updated.log", updatedSupplier.getId()));

        return supplierMapper.toDto(updatedSupplier);
    }

    /**
     * Retrieves a paginated list of suppliers
     *
     * @param name   supplier name filter (optional)
     * @param status supplier status filter (optional)
     * @param page   page number
     * @param size   page size
     * @return page of supplier DTOs
     */
    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(String name, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Supplier> suppliers = supplierRepository.searchSuppliersNative(name, status, pageable);

        log.debug(logMsg.get("manager.suppliers.fetched.log", suppliers.getTotalElements()));

        return suppliers.map(supplierMapper::toDto);
    }

    // =========================================================================
    // SUPPLIER PRODUCT MANAGEMENT
    // =========================================================================

    /**
     * Adds a product to a supplier's catalog
     *
     * @param supplierId supplier identifier
     * @param productId  product identifier
     * @param request    supplier product request with price and SKU
     * @param managerId  ID of the manager adding the product
     * @return created supplier product DTO
     */
    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId,
                                                   Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId) {
        return supplierProductProcessor.addProductToSupplier(supplierId, productId, request, managerId);
    }

    // =========================================================================
    // PURCHASE ORDER QUERIES
    // =========================================================================

    /**
     * Retrieves a paginated list of purchase orders with filters
     *
     * @param supplierId supplier ID filter (optional)
     * @param status     order status filter (optional)
     * @param startDate  start date filter (optional)
     * @param endDate    end date filter (optional)
     * @param page       page number
     * @param size       page size
     * @return page of purchase order DTOs
     */
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

    /**
     * Retrieves a purchase order by ID
     *
     * @param orderId purchase order identifier
     * @return purchase order DTO
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long orderId) {
        PurchaseOrder order = purchaseOrderValidator.validatePurchaseOrderExists(orderId);
        String supplierName = supplierRepository.findById(order.getSupplierId())
                .map(Supplier::getName)
                .orElse(null);
        return purchaseOrderConverter.toDto(order, supplierName);
    }
}