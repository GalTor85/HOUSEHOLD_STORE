package ru.galtor85.household_store.util.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.order.OrderItemNotFoundException;
import ru.galtor85.household_store.advice.exception.order.OrderNotFoundException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderDetailsNotFoundException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFromSupplierException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierInactiveException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierProductNotFoundException;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.order.SalesOrder;
import ru.galtor85.household_store.entity.order.SalesOrderItem;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

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