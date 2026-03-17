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
import ru.galtor85.household_store.builder.SupplierProductBuilder;
import ru.galtor85.household_store.converter.OrderConverter;
import ru.galtor85.household_store.converter.SupplierProductConverter;
import ru.galtor85.household_store.dto.*;
        import ru.galtor85.household_store.entity.*;
        import ru.galtor85.household_store.mapper.SupplierMapper;
import ru.galtor85.household_store.repository.*;
        import ru.galtor85.household_store.util.*;

        import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPurchaseService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;

    // Мапперы
    private final SupplierMapper supplierMapper;

    // Конвертеры
    private final OrderConverter orderConverter;
    private final SupplierProductConverter supplierProductConverter;

    // Билдеры
    private final OrderBuilder orderBuilder;
    private final PurchaseOrderBuilder purchaseOrderBuilder;
    private final SupplierProductBuilder supplierProductBuilder;

    // Утилиты
    private final EntityFinder entityFinder;
    private final ValidationHelper validationHelper;
    private final DateParser dateParser;

    // ========== PURCHASE ORDER CREATION ==========

    @Transactional
    public OrderDto createPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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

        return orderConverter.convertToDto(savedOrder, locale);
    }

    @Transactional
    public OrderDto receivePurchaseOrder(Long orderId, ReceiveOrderRequest request,
                                         Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = entityFinder.findPurchaseOrderById(orderId);
        validationHelper.validateOrderForReceiving(order);

        PurchaseOrder purchaseOrder = entityFinder.findPurchaseOrderDetails(orderId);
        updateStockFromOrder(order);
        purchaseOrderBuilder.updateForReceiving(purchaseOrder, request, managerId);

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        purchaseOrderRepository.save(purchaseOrder);

        log.info(messageService.get("manager.purchase.received.log", orderId, managerId));

        return orderConverter.convertToDto(order, locale);
    }

    // ========== STOCK WRITE-OFF ==========

    @Transactional
    public void writeOffStock(StockWriteOffRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        for (StockWriteOffItem item : request.getItems()) {
            Product product = entityFinder.findProductById(item.getProductId());
            validationHelper.validateStockAvailability(product, item.getQuantity());

            product.setQuantityInStock(product.getQuantityInStock() - item.getQuantity());
            productRepository.save(product);

            log.info(messageService.get("manager.writeoff.executed.log",
                    item.getProductId(), item.getQuantity(), request.getReason(), managerId));
        }
    }

    // ========== SUPPLIER MANAGEMENT ==========

    @Transactional
    public SupplierDto createSupplier(SupplierCreateRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        validationHelper.validateSupplierUniqueness(request);

        Supplier supplier = supplierMapper.toEntity(request, managerId);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get("manager.supplier.created.log",
                savedSupplier.getName(), savedSupplier.getId(), managerId));

        return supplierMapper.toDto(savedSupplier, locale);
    }

    @Transactional
    public SupplierDto updateSupplier(Long supplierId, SupplierUpdateRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Supplier supplier = entityFinder.findSupplierById(supplierId);
        validationHelper.validateSupplierUniquenessOnUpdate(supplier, request);

        supplierMapper.updateEntity(supplier, request);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get("manager.supplier.updated.log", updatedSupplier.getId()));

        return supplierMapper.toDto(updatedSupplier, locale);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(String name, String status, int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Supplier> suppliers = supplierRepository.searchSuppliersNative(name, status, pageable);

        log.debug(messageService.get("manager.suppliers.fetched.log", suppliers.getTotalElements()));

        Locale finalLocale = locale;
        return suppliers.map(supplier -> supplierMapper.toDto(supplier, finalLocale));
    }

    // ========== SUPPLIER PRODUCT MANAGEMENT ==========

    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId, Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        entityFinder.findSupplierById(supplierId);
        entityFinder.findProductById(productId);
        validationHelper.validateProductNotLinked(supplierId, productId);

        SupplierProduct supplierProduct = supplierProductBuilder.buildFromRequest(request, supplierId, productId);

        if (request.getMainSupplier()) {
            supplierProductBuilder.resetMainSupplierFlag(productId);
        }

        SupplierProduct saved = supplierProductRepository.save(supplierProduct);

        log.info(messageService.get("manager.supplier.product.added.log", productId, supplierId, managerId));

        return supplierProductConverter.convertToDto(saved, productId, supplierId, locale);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long supplierProductId,
                                                    SupplierProductRequest request,
                                                    Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SupplierProduct supplierProduct = entityFinder.findSupplierProductById(supplierProductId);
        supplierProductBuilder.updateFromRequest(supplierProduct, request);

        if (request.getMainSupplier() && !supplierProduct.getMainSupplier()) {
            supplierProductBuilder.resetMainSupplierFlag(supplierProduct.getProductId());
            supplierProduct.setMainSupplier(true);
        }

        SupplierProduct updated = supplierProductRepository.save(supplierProduct);

        log.info(messageService.get("manager.supplier.product.updated.log", supplierProductId, managerId));

        return supplierProductConverter.convertToDto(updated,
                updated.getProductId(), updated.getSupplierId(), locale);
    }

    @Transactional
    public void removeProductFromSupplier(Long supplierProductId, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SupplierProduct supplierProduct = entityFinder.findSupplierProductById(supplierProductId);
        supplierProductRepository.delete(supplierProduct);

        log.info(messageService.get("manager.supplier.product.removed.log",
                supplierProduct.getProductId(), supplierProduct.getSupplierId(), managerId));
    }

    // ========== PURCHASE ORDER QUERIES ==========

    @Transactional(readOnly = true)
    public Page<OrderDto> getPurchaseOrders(Long supplierId, String status,
                                            String startDate, String endDate,
                                            int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime start = dateParser.parseDate(startDate);
        LocalDateTime end = dateParser.parseDate(endDate);

        validationHelper.validateDateRange(start, end, startDate, endDate);

        OrderStatus orderStatus = dateParser.parseOrderStatus(status);
        Page<Order> orders = orderRepository.searchOrders(
                null, supplierId, orderStatus, OrderType.PURCHASE, start, end, pageable);

        log.debug(messageService.get("manager.purchase.fetched.log", orders.getTotalElements()));

        Locale finalLocale = locale;
        return orders.map(order -> orderConverter.convertToDto(order, finalLocale));
    }

    @Transactional(readOnly = true)
    public OrderDto getPurchaseOrderById(Long orderId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = entityFinder.findPurchaseOrderById(orderId);
        return orderConverter.convertToDto(order, locale);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void updateStockFromOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = entityFinder.findProductById(item.getProductId());
            product.setQuantityInStock(product.getQuantityInStock() + item.getQuantity());
            productRepository.save(product);

            log.debug(messageService.get("manager.purchase.stock.updated.log",
                    product.getId(), product.getQuantityInStock() - item.getQuantity(),
                    product.getQuantityInStock()));
        }
    }
}