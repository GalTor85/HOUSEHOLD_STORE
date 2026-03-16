package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPurchaseService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;

    // ========== PURCHASE ORDER CREATION ==========

    @Transactional
    public OrderDto createPurchaseOrder(PurchaseOrderCreateRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверяем существование поставщика
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.log.not.found", request.getSupplierId()));
                    return new SupplierNotFoundException(request.getSupplierId());
                });

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.supplier.log.inactive", supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }

        // Генерируем номер заказа
        String orderNumber = generatePurchaseOrderNumber();

        // Создаем основной заказ
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .supplierId(request.getSupplierId())
                .orderType(OrderType.PURCHASE)
                .status(OrderStatus.PENDING)
                .createdBy(managerId)
                .notes(request.getNotes())
                .build();

        // Добавляем товары в заказ
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderItemDto itemDto : request.getItems()) {
            // Проверяем товар
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get("manager.product.log.not.found", itemDto.getProductId()));
                        return new ProductNotFoundException(itemDto.getProductId());
                    });

            // Проверяем, что товар поставляется этим поставщиком
            SupplierProduct supplierProduct = supplierProductRepository
                    .findBySupplierIdAndProductId(request.getSupplierId(), itemDto.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get(
                                "manager.purchase.log.product.not.from.supplier",
                                itemDto.getProductId(),
                                request.getSupplierId()
                        ));
                        return new ProductNotFromSupplierException(itemDto.getProductId(), request.getSupplierId());
                    });

            BigDecimal itemTotal = supplierProduct.getSupplierPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .supplierProductId(supplierProduct.getId())
                    .quantity(itemDto.getQuantity())
                    .price(supplierProduct.getSupplierPrice())
                    .supplierPrice(supplierProduct.getSupplierPrice())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .supplierSku(supplierProduct.getSupplierSku())
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setSubtotal(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Создаем запись в purchase_orders
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .order(savedOrder)
                .expectedDelivery(request.getExpectedDelivery())
                .warehouseLocation(request.getWarehouseLocation())
                .invoiceNumber(request.getInvoiceNumber())
                .paymentDue(request.getPaymentDue())
                .paymentStatus("PENDING")
                .build();

        purchaseOrderRepository.save(purchaseOrder);

        log.info(messageService.get(
                "manager.purchase.created.log",
                orderNumber,
                request.getSupplierId(),
                managerId
        ));

        return convertToDto(savedOrder, locale);
    }

    @Transactional
    public OrderDto receivePurchaseOrder(Long orderId, ReceiveOrderRequest request,
                                         Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getOrderType() != OrderType.PURCHASE) {
            log.error(messageService.get("manager.purchase.log.not.purchase.order", orderId));
            throw new InvalidOrderTypeException(orderId, "PURCHASE");
        }

        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.PENDING) {
            log.error(messageService.get("manager.purchase.log.cannot.receive", order.getStatus()));
            throw new CannotReceivePurchaseOrderException(order.getStatus());
        }

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.purchase.log.purchase.details.not.found", orderId));
                    return new PurchaseOrderDetailsNotFoundException(orderId);
                });

        // Обновляем остатки товаров на складе
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get("manager.product.log.not.found", item.getProductId()));
                        return new ProductNotFoundException(item.getProductId());
                    });

            int newQuantity = product.getQuantityInStock() + item.getQuantity();
            product.setQuantityInStock(newQuantity);
            productRepository.save(product);

            log.debug(messageService.get(
                    "manager.purchase.stock.updated.log",
                    product.getId(),
                    product.getQuantityInStock(),
                    newQuantity
            ));
        }

        // Обновляем статусы
        order.setStatus(OrderStatus.DELIVERED);
        purchaseOrder.setActualDelivery(LocalDate.from(request.getReceivedAt() != null ?
                request.getReceivedAt() : LocalDateTime.now()));
        purchaseOrder.setReceivedBy(managerId);
        purchaseOrder.setQualityCheck(request.getQualityCheck());
        purchaseOrder.setPaymentStatus(request.getPaymentStatus() != null ?
                request.getPaymentStatus() : purchaseOrder.getPaymentStatus());

        purchaseOrderRepository.save(purchaseOrder);
        Order updatedOrder = orderRepository.save(order);

        log.info(messageService.get(
                "manager.purchase.received.log",
                orderId,
                managerId
        ));

        return convertToDto(updatedOrder, locale);
    }

    // ========== STOCK WRITE-OFF ==========

    @Transactional
    public void writeOffStock(StockWriteOffRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        for (StockWriteOffItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> {
                        log.error(messageService.get("manager.product.log.not.found", item.getProductId()));
                        return new ProductNotFoundException(item.getProductId());
                    });

            if (product.getQuantityInStock() < item.getQuantity()) {
                log.error(messageService.get(
                        "manager.writeoff.log.insufficient.stock",
                        item.getProductId(),
                        product.getQuantityInStock(),
                        item.getQuantity()
                ));
                throw new WriteOffInsufficientStockException(
                        item.getProductId(),
                        product.getQuantityInStock(),
                        item.getQuantity()
                );
            }

            int newQuantity = product.getQuantityInStock() - item.getQuantity();
            product.setQuantityInStock(newQuantity);
            productRepository.save(product);

            // Здесь можно добавить запись в таблицу write_off_logs
            log.info(messageService.get(
                    "manager.writeoff.executed.log",
                    item.getProductId(),
                    item.getQuantity(),
                    request.getReason(),
                    managerId
            ));
        }
    }

    // ========== SUPPLIER MANAGEMENT ==========

    @Transactional
    public SupplierDto createSupplier(SupplierCreateRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверка уникальности email
        if (request.getEmail() != null && supplierRepository.existsByEmail(request.getEmail())) {
            log.warn(messageService.get("manager.supplier.log.email.exists", request.getEmail()));
            throw new SupplierAlreadyExistsException("email", request.getEmail());
        }

        // Проверка уникальности ИНН
        if (request.getInn() != null && supplierRepository.existsByInn(request.getInn())) {
            log.warn(messageService.get("manager.supplier.log.inn.exists", request.getInn()));
            throw new SupplierAlreadyExistsException("inn", request.getInn());
        }

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .contactPerson(request.getContactPerson())
                .inn(request.getInn())
                .kpp(request.getKpp())
                .ogrn(request.getOgrn())
                .legalAddress(request.getLegalAddress())
                .actualAddress(request.getActualAddress())
                .bankName(request.getBankName())
                .bankBic(request.getBankBic())
                .bankAccount(request.getBankAccount())
                .correspondentAccount(request.getCorrespondentAccount())
                .status(SupplierStatus.PENDING)
                .deliveryTime(request.getDeliveryTime())
                .minOrderAmount(request.getMinOrderAmount())
                .paymentDelay(request.getPaymentDelay())
                .createdBy(managerId)
                .build();

        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get(
                "manager.supplier.created.log",
                savedSupplier.getName(),
                savedSupplier.getId(),
                managerId
        ));

        return convertSupplierToDto(savedSupplier, locale);
    }

    @Transactional
    public SupplierDto updateSupplier(Long supplierId, SupplierUpdateRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.log.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });

        // Проверка уникальности email при изменении
        if (request.getEmail() != null && !request.getEmail().equals(supplier.getEmail())) {
            if (supplierRepository.existsByEmail(request.getEmail())) {
                log.warn(messageService.get("manager.supplier.log.email.exists", request.getEmail()));
                throw new SupplierAlreadyExistsException("email", request.getEmail());
            }
            supplier.setEmail(request.getEmail());
        }

        // Проверка уникальности ИНН при изменении
        if (request.getInn() != null && !request.getInn().equals(supplier.getInn())) {
            if (supplierRepository.existsByInn(request.getInn())) {
                log.warn(messageService.get("manager.supplier.log.inn.exists", request.getInn()));
                throw new SupplierAlreadyExistsException("inn", request.getInn());
            }
            supplier.setInn(request.getInn());
        }

        // Обновление полей
        if (request.getName() != null) supplier.setName(request.getName());
        if (request.getPhone() != null) supplier.setPhone(request.getPhone());
        if (request.getWebsite() != null) supplier.setWebsite(request.getWebsite());
        if (request.getContactPerson() != null) supplier.setContactPerson(request.getContactPerson());
        if (request.getKpp() != null) supplier.setKpp(request.getKpp());
        if (request.getOgrn() != null) supplier.setOgrn(request.getOgrn());
        if (request.getLegalAddress() != null) supplier.setLegalAddress(request.getLegalAddress());
        if (request.getActualAddress() != null) supplier.setActualAddress(request.getActualAddress());
        if (request.getBankName() != null) supplier.setBankName(request.getBankName());
        if (request.getBankBic() != null) supplier.setBankBic(request.getBankBic());
        if (request.getBankAccount() != null) supplier.setBankAccount(request.getBankAccount());
        if (request.getCorrespondentAccount() != null) supplier.setCorrespondentAccount(request.getCorrespondentAccount());
        if (request.getStatus() != null) supplier.setStatus(SupplierStatus.valueOf(request.getStatus()));
        if (request.getDeliveryTime() != null) supplier.setDeliveryTime(request.getDeliveryTime());
        if (request.getMinOrderAmount() != null) supplier.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getPaymentDelay() != null) supplier.setPaymentDelay(request.getPaymentDelay());

        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info(messageService.get(
                "manager.supplier.updated.log",
                updatedSupplier.getId()
        ));

        return convertSupplierToDto(updatedSupplier, locale);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(String name, String status, int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(page, size, sort);

        // Преобразуем статус в строку, если он передан
        String statusStr = status != null ? status : null;

        Page<Supplier> suppliers = supplierRepository.searchSuppliersNative(name, statusStr, pageable);

        log.debug(messageService.get("manager.suppliers.fetched.log", suppliers.getTotalElements()));

        Locale finalLocale = locale;
        return suppliers.map(supplier -> convertSupplierToDto(supplier, finalLocale));
    }

    // ========== SUPPLIER PRODUCT MANAGEMENT ==========

    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId, Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.log.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        // Проверяем, не добавлен ли уже этот товар
        if (supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId).isPresent()) {
            log.warn(messageService.get("manager.supplier.log.product.already.added", productId, supplierId));
            throw new SupplierProductAlreadyExistsException(productId, supplierId);
        }

        SupplierProduct supplierProduct = SupplierProduct.builder()
                .supplierId(supplierId)
                .productId(productId)
                .supplierPrice(request.getSupplierPrice())
                .supplierSku(request.getSupplierSku())
                .mainSupplier(request.getMainSupplier())
                .deliveryTime(request.getDeliveryTime())
                .minOrderQuantity(request.getMinOrderQuantity())
                .build();

        // Если это основной поставщик, сбрасываем флаг у других
        if (request.getMainSupplier()) {
            supplierProductRepository.findByProductId(productId)
                    .forEach(sp -> {
                        sp.setMainSupplier(false);
                        supplierProductRepository.save(sp);
                    });
        }

        SupplierProduct saved = supplierProductRepository.save(supplierProduct);

        log.info(messageService.get(
                "manager.supplier.product.added.log",
                productId,
                supplierId,
                managerId
        ));

        return convertSupplierProductToDto(saved, product, supplier, locale);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long supplierProductId,
                                                    SupplierProductRequest request,
                                                    Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SupplierProduct supplierProduct = supplierProductRepository.findById(supplierProductId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.product.log.not.found", supplierProductId));
                    return new SupplierProductNotFoundException(supplierProductId);
                });

        if (request.getSupplierPrice() != null) {
            supplierProduct.setSupplierPrice(request.getSupplierPrice());
        }
        if (request.getSupplierSku() != null) {
            supplierProduct.setSupplierSku(request.getSupplierSku());
        }
        if (request.getDeliveryTime() != null) {
            supplierProduct.setDeliveryTime(request.getDeliveryTime());
        }
        if (request.getMinOrderQuantity() != null) {
            supplierProduct.setMinOrderQuantity(request.getMinOrderQuantity());
        }

        // Обработка флага основного поставщика
        if (request.getMainSupplier() && !supplierProduct.getMainSupplier()) {
            supplierProductRepository.findByProductId(supplierProduct.getProductId())
                    .forEach(sp -> {
                        sp.setMainSupplier(false);
                        supplierProductRepository.save(sp);
                    });
            supplierProduct.setMainSupplier(true);
        }

        SupplierProduct updated = supplierProductRepository.save(supplierProduct);

        Product product = productRepository.findById(updated.getProductId()).orElse(null);
        Supplier supplier = supplierRepository.findById(updated.getSupplierId()).orElse(null);

        log.info(messageService.get(
                "manager.supplier.product.updated.log",
                supplierProductId,
                managerId
        ));

        return convertSupplierProductToDto(updated, product, supplier, locale);
    }

    @Transactional
    public void removeProductFromSupplier(Long supplierProductId, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        SupplierProduct supplierProduct = supplierProductRepository.findById(supplierProductId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.product.log.not.found", supplierProductId));
                    return new SupplierProductNotFoundException(supplierProductId);
                });

        supplierProductRepository.delete(supplierProduct);

        log.info(messageService.get(
                "manager.supplier.product.removed.log",
                supplierProduct.getProductId(),
                supplierProduct.getSupplierId(),
                managerId
        ));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getPurchaseOrders(Long supplierId, String status,
                                            String startDate, String endDate,
                                            int page, int size, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Создаем final переменные для использования в лямбде
        final Locale finalLocale = locale;
        final Long finalSupplierId = supplierId;
        final String finalStatus = status;
        final String finalStartDate = startDate;
        final String finalEndDate = endDate;
        final int finalPage = page;
        final int finalSize = size;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(finalPage, finalSize, sort);

        LocalDateTime start = parseDate(finalStartDate);
        LocalDateTime end = parseDate(finalEndDate);

        OrderStatus orderStatus = null;
        if (finalStatus != null && !finalStatus.isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(finalStatus);
            } catch (IllegalArgumentException e) {
                log.warn(messageService.get("manager.order.log.invalid.status", finalStatus));
            }
        }

        Page<Order> orders = orderRepository.searchOrders(
                null, finalSupplierId, orderStatus, OrderType.PURCHASE, start, end, pageable);

        log.debug(messageService.get(
                "manager.purchase.fetched.log",
                orders.getTotalElements()
        ));

        return orders.map(order -> convertToDto(order, finalLocale));
    }

    @Transactional(readOnly = true)
    public OrderDto getPurchaseOrderById(Long orderId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", orderId));
                    return new OrderNotFoundException(orderId);
                });

        if (order.getOrderType() != OrderType.PURCHASE) {
            log.error(messageService.get("manager.purchase.log.not.purchase.order", orderId));
            throw new InvalidOrderTypeException(orderId, "PURCHASE");
        }

        return convertToDto(order, locale);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private String generatePurchaseOrderNumber() {
        return "PO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            log.warn(messageService.get("manager.purchase.log.date.parse.failed", dateStr));
            return null;
        }
    }

    private OrderDto convertToDto(Order order, Locale locale) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());

        String localizedStatus = messageService.get("order.status." + order.getStatus().name());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .supplierId(order.getSupplierId())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .localizedStatus(localizedStatus)
                .items(itemDtos)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .createdAt(order.getCreatedAt())
                .notes(order.getNotes())
                .build();
    }

    private OrderItemDto convertItemToDto(OrderItem item) {
        return OrderItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private SupplierDto convertSupplierToDto(Supplier supplier, Locale locale) {
        String localizedStatus = messageService.get("supplier.status." + supplier.getStatus().name());

        return SupplierDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .website(supplier.getWebsite())
                .contactPerson(supplier.getContactPerson())
                .inn(supplier.getInn())
                .kpp(supplier.getKpp())
                .ogrn(supplier.getOgrn())
                .legalAddress(supplier.getLegalAddress())
                .actualAddress(supplier.getActualAddress())
                .bankName(supplier.getBankName())
                .bankBic(supplier.getBankBic())
                .bankAccount(supplier.getBankAccount())
                .correspondentAccount(supplier.getCorrespondentAccount())
                .status(supplier.getStatus())
                .localizedStatus(localizedStatus)
                .rating(supplier.getRating())
                .ratingCount(supplier.getRatingCount())
                .deliveryTime(supplier.getDeliveryTime())
                .minOrderAmount(supplier.getMinOrderAmount())
                .paymentDelay(supplier.getPaymentDelay())
                .createdAt(supplier.getCreatedAt())
                .build();
    }

    private SupplierProductDto convertSupplierProductToDto(SupplierProduct sp, Product product,
                                                           Supplier supplier, Locale locale) {
        return SupplierProductDto.builder()
                .id(sp.getId())
                .supplierId(sp.getSupplierId())
                .supplierName(supplier != null ? supplier.getName() : null)
                .productId(sp.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .supplierPrice(sp.getSupplierPrice())
                .supplierSku(sp.getSupplierSku())
                .mainSupplier(sp.getMainSupplier())
                .deliveryTime(sp.getDeliveryTime())
                .minOrderQuantity(sp.getMinOrderQuantity())
                .build();
    }
}