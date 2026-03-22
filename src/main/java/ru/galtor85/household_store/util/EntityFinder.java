package ru.galtor85.household_store.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.*;
import ru.galtor85.household_store.service.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityFinder {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final MessageService messageService;

    public Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.log.not.found", id));
                    return new SupplierNotFoundException(id);
                });
    }

    public Supplier findActiveSupplierById(Long id) {
        Supplier supplier = findSupplierById(id);
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.supplier.log.inactive", supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }
        return supplier;
    }

    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", id));
                    return new ProductNotFoundException(id);
                });
    }

    public Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", id));
                    return new OrderNotFoundException(id);
                });
    }

    public Order findPurchaseOrderById(Long id) {
        Order order = findOrderById(id);
        if (order.getOrderType() != OrderType.PURCHASE) {
            log.error(messageService.get("manager.purchase.log.not.purchase.order", id));
            throw new InvalidOrderTypeException(id, "PURCHASE");
        }
        return order;
    }

    public PurchaseOrder findPurchaseOrderDetails(Long orderId) {
        return purchaseOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.purchase.log.purchase.details.not.found", orderId));
                    return new PurchaseOrderDetailsNotFoundException(orderId);
                });
    }

    public SupplierProduct findSupplierProductById(Long id) {
        return supplierProductRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.product.log.not.found", id));
                    return new SupplierProductNotFoundException(id);
                });
    }

    public SupplierProduct findSupplierProductBySupplierAndProduct(Long supplierId, Long productId) {
        return supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.purchase.log.product.not.from.supplier",
                            productId, supplierId));
                    return new ProductNotFromSupplierException(productId, supplierId);
                });
    }

    public Order findCustomerOrderById(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderType() != OrderType.RETAIL && order.getOrderType() != OrderType.WHOLESALE) {
            log.error(messageService.get("manager.order.log.not.customer.order", orderId));
            throw new InvalidOrderTypeException(orderId, "RETAIL or WHOLESALE");
        }
        return order;
    }

    public OrderItem findOrderItem(Order order, Long productId) {
        return order.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))  // ← ищем по productId
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("order.item.not.found.by.product", productId, order.getId()));
                    return new OrderItemNotFoundException(productId);
                });
    }

    /**
     * Поиск позиции заказа по ID продукта
     */
    public OrderItem findOrderItemByProductId(Order order, Long productId) {
        return order.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("order.item.not.found.by.product", productId, order.getId()));
                    return new OrderItemNotFoundException(productId);
                });
    }
}