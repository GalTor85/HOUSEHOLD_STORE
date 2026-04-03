package ru.galtor85.household_store.validator.order;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.order.CannotReceivePurchaseOrderException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderCancellationException;
import ru.galtor85.household_store.advice.exception.order.PurchaseOrderNotFoundException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierInactiveException;
import ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.dto.request.order.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.common.PurchaseOrderItemCreateDto;
import ru.galtor85.household_store.entity.order.OrderStatus;
import ru.galtor85.household_store.entity.order.PurchaseOrder;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierStatus;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.order.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for purchase order operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MessageService messageService;

    // =========================================================================
    // CREATE REQUEST VALIDATION
    // =========================================================================

    /**
     * Validates the purchase order creation request
     *
     * @param request purchase order creation request
     * @throws IllegalArgumentException if request is invalid
     */
    public void validateCreateRequest(PurchaseOrderCreateRequest request) {
        if (request == null) {
            log.error(messageService.get("purchase.validator.request.null"));
            throw new IllegalArgumentException(
                    messageService.get("purchase.validator.request.null")
            );
        }

        validateNotEmpty(request);

        if (request.getSupplierId() == null) {
            log.error(messageService.get("purchase.validator.supplier.id.empty"));
            throw new IllegalArgumentException(
                    messageService.get("purchase.validator.supplier.id.empty")
            );
        }
    }

    /**
     * Validates that the order is not empty
     *
     * @param request purchase order creation request
     * @throws IllegalArgumentException if order has no items
     */
    public void validateNotEmpty(PurchaseOrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error(messageService.get("purchase.validation.items.empty"));
            throw new IllegalArgumentException(
                    messageService.get("purchase.validation.items.empty")
            );
        }
    }

    // =========================================================================
    // SUPPLIER VALIDATION
    // =========================================================================

    /**
     * Validates that a supplier exists by ID
     *
     * @param supplierId supplier identifier
     * @return supplier entity
     * @throws SupplierNotFoundException if supplier not found
     */
    public Supplier validateSupplierExists(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.error.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });
    }

    /**
     * Validates that a supplier exists and is active
     *
     * @param supplierId supplier identifier
     * @return supplier entity
     * @throws SupplierNotFoundException if supplier not found
     * @throws SupplierInactiveException if supplier is not active
     */
    public Supplier validateSupplierActive(Long supplierId) {
        Supplier supplier = validateSupplierExists(supplierId);

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.purchase.error.supplier.inactive",
                    supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }

        return supplier;
    }

    /**
     * Validates that a supplier is active
     *
     * @param supplier supplier entity
     * @throws SupplierInactiveException if supplier is not active
     */
    public void validateSupplierActive(Supplier supplier) {
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            log.error(messageService.get("manager.purchase.error.supplier.inactive",
                    supplier.getStatus()));
            throw new SupplierInactiveException(supplier.getStatus());
        }
    }

    // =========================================================================
    // PRODUCT VALIDATION
    // =========================================================================

    /**
     * Validates all products in the order and returns products with prices
     *
     * @param items list of order items
     * @return validation result with products and prices
     * @throws ProductNotFoundException if product not found
     * @throws InvalidPriceException if price is invalid
     */
    public ProductValidationResult validateProducts(List<PurchaseOrderItemCreateDto> items) {
        List<Product> products = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();

        for (PurchaseOrderItemCreateDto item : items) {
            Product product = validateProductExists(item.getProductId());
            BigDecimal price = determinePrice(product, item);
            validatePrice(price, product);

            products.add(product);
            prices.add(price);
        }

        return new ProductValidationResult(products, prices);
    }

    /**
     * Validates that a product exists by ID
     *
     * @param productId product identifier
     * @return product entity
     * @throws ProductNotFoundException if product not found
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.error.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Determines the price for a product (custom price or supplier price)
     *
     * @param product product entity
     * @param item order item DTO
     * @return determined price
     */
    private BigDecimal determinePrice(Product product, PurchaseOrderItemCreateDto item) {
        if (item.getCustomPrice() != null) {
            return item.getCustomPrice();
        }
        return product.getSupplierPrice();
    }

    /**
     * Validates that a price is positive
     *
     * @param price price to validate
     * @param product product entity (for logging)
     * @throws InvalidPriceException if price is invalid
     */
    private void validatePrice(BigDecimal price, Product product) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("manager.purchase.error.price.not.set",
                    product.getSku()));
            throw new InvalidPriceException(price);
        }
    }

    // =========================================================================
    // PURCHASE ORDER VALIDATION
    // =========================================================================

    /**
     * Validates that a purchase order exists by ID
     *
     * @param orderId purchase order identifier
     * @return purchase order entity
     * @throws PurchaseOrderNotFoundException if order not found
     */
    public PurchaseOrder validatePurchaseOrderExists(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("purchase.order.not.found", orderId));
                    return new PurchaseOrderNotFoundException(orderId);
                });
    }

    /**
     * Validates that a purchase order can be received
     *
     * @param order purchase order entity
     * @throws CannotReceivePurchaseOrderException if order cannot be received
     */
    public void validateOrderForReceiving(PurchaseOrder order) {
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.COMPLETED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            log.error(messageService.get("manager.purchase.error.cannot.receive",
                    order.getStatus()));
            throw new CannotReceivePurchaseOrderException(order.getStatus());
        }
    }

    /**
     * Validates that a purchase order can be cancelled
     *
     * @param order purchase order entity
     * @throws PurchaseOrderCancellationException if order cannot be cancelled
     */
    public void validateOrderCancellable(PurchaseOrder order) {
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.COMPLETED||
                order.getStatus() == OrderStatus.CANCELLED) {
            log.error(messageService.get("purchase.order.cannot.cancel", order.getStatus()));
            throw new PurchaseOrderCancellationException(
                    messageService.get("purchase.order.cannot.cancel", order.getStatus())
            );
        }
        // Cannot cancel if already partially received
        if (order.getStatus() == OrderStatus.PARTIALLY_RECEIVED) {
            log.error(messageService.get("purchase.order.cannot.cancel.partially.received",
                    order.getId()));
            throw new PurchaseOrderCancellationException(
                    messageService.get("purchase.order.cannot.cancel.partially.received",
                            order.getId())
            );
        }
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Result of product validation containing products and their prices
     */
    @Value
    public static class ProductValidationResult {
        List<Product> products;
        List<BigDecimal> prices;
    }
}