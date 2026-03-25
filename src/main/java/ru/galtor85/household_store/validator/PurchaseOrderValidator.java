package ru.galtor85.household_store.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.PurchaseOrderCreateRequest;
import ru.galtor85.household_store.dto.PurchaseOrderItemCreateDto;
import ru.galtor85.household_store.entity.*;
import ru.galtor85.household_store.repository.ProductRepository;
import ru.galtor85.household_store.repository.PurchaseOrderRepository;
import ru.galtor85.household_store.repository.SupplierRepository;
import ru.galtor85.household_store.repository.WarehouseRepository;
import ru.galtor85.household_store.service.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final MessageService messageService;



    // =========================================================================
    // ВАЛИДАЦИЯ ПОСТАВЩИКА
    // =========================================================================

    /**
     * Проверяет существование поставщика
     */
    public Supplier validateSupplierExists(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.supplier.error.not.found", supplierId));
                    return new SupplierNotFoundException(supplierId);
                });
    }

    /**
     * Проверяет существование и активность поставщика
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

    // =========================================================================
    // ВАЛИДАЦИЯ ТОВАРОВ
    // =========================================================================

    /**
     * Проверяет все товары в заказе и возвращает список продуктов и цен
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
     * Проверяет существование товара
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.error.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Определяет цену товара (кастомная или из поставщика)
     */
    private BigDecimal determinePrice(Product product, PurchaseOrderItemCreateDto item) {
        if (item.getCustomPrice() != null) {
            return item.getCustomPrice();
        }
        return product.getSupplierPrice();
    }

    /**
     * Проверяет цену
     */
    private void validatePrice(BigDecimal price, Product product) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(messageService.get("manager.purchase.error.price.not.set",
                    product.getSku()));
            throw new InvalidPriceException(price);
        }
    }

    // =========================================================================
    // ВАЛИДАЦИЯ ЗАКАЗА
    // =========================================================================

    /**
     * Проверяет, что заказ не пустой
     */
    public void validateNotEmpty(PurchaseOrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error(messageService.get("purchase.validation.items.empty"));
            throw new IllegalArgumentException(
                    messageService.get("purchase.validation.items.empty")
            );
        }
    }

    /**
     * Проверяет существование заказа на закупку
     */
    public PurchaseOrder validatePurchaseOrderExists(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.purchase.log.not.found", orderId));
                    return new PurchaseOrderNotFoundException(orderId);
                });
    }

    /**
     * Проверяет, что заказ можно принимать
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
     * Проверяет существование склада
     */
    public void validateWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            log.error(messageService.get("receive.validation.warehouse.id.null"));
            throw new IllegalArgumentException(
                    messageService.get("receive.validation.warehouse.id.null")
            );
        }

        if (!warehouseRepository.existsById(warehouseId)) {
            log.error(messageService.get("warehouse.error.not.found.id", warehouseId));
            throw new WarehouseNotFoundException(warehouseId);
        }
    }

    // =========================================================================
    // ВНУТРЕННИЕ КЛАССЫ
    // =========================================================================

    /**
     * Результат валидации товаров
     */
    @lombok.Value
    public static class ProductValidationResult {
        List<Product> products;
        List<BigDecimal> prices;
    }
}