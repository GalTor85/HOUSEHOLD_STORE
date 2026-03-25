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
    private final SalesOrderRepository salesOrderRepository;
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

    public SalesOrder findOrderById(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.order.log.not.found", id));
                    return new OrderNotFoundException(id);
                });
    }

    /**
     * Находит закупку по ID
     */
    public PurchaseOrder findPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.purchase.log.not.found", id));
                    return new PurchaseOrderNotFoundException(id);
                });
    }

    public PurchaseOrder findPurchaseOrderDetails(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
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



    public SalesOrderItem findSalesOrderItem(SalesOrder salesOrder, Long productId) {
        return salesOrder.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))  // ← ищем по productId
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("salesOrder.item.not.found.by.product", productId, salesOrder.getId()));
                    return new OrderItemNotFoundException(productId);
                });
    }

    /**
     * Поиск позиции заказа по ID продукта
     */
    public SalesOrderItem findSalesOrderItemByProductId(SalesOrder salesOrder, Long productId) {
        return salesOrder.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(messageService.get("salesOrder.item.not.found.by.product", productId, salesOrder.getId()));
                    return new OrderItemNotFoundException(productId);
                });
    }
}