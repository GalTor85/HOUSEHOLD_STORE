package ru.galtor85.household_store.validator.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.product.ProductInactiveException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.cart.CartItemRepository;
import ru.galtor85.household_store.repository.order.SalesOrderItemRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;

/**
 * Validator for product operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidator {

    private final ProductRepository productRepository;
    private final LogMessageService logMsg;
    private final MessageService messageService;
    private final CartItemRepository cartItemRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;


    /**
     * Validates product exists by ID.
     *
     * @param productId product ID
     * @return product entity
     * @throws ProductNotFoundException if not found
     */
    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Validates SKU is unique.
     *
     * @param sku product SKU
     * @throws ProductAlreadyExistsException if already exists
     */
    public void validateUniqueSku(String sku) {
        if (productRepository.existsBySku(sku)) {
            log.warn(logMsg.get("manager.product.log.sku.exists", sku));
            throw new ProductAlreadyExistsException("SKU", sku);
        }
    }

    /**
     * Validates barcode is unique.
     *
     * @param barcode product barcode
     * @throws ProductAlreadyExistsException if already exists
     */
    public void validateUniqueBarcode(String barcode) {
        if (barcode != null && !barcode.isEmpty() && productRepository.existsByBarcode(barcode)) {
            log.warn(logMsg.get("manager.product.log.barcode.exists", barcode));
            throw new ProductAlreadyExistsException("barcode", barcode);
        }
    }

    /**
     * Validates SKU uniqueness for update.
     *
     * @param newSku new SKU value
     * @param oldSku current SKU value
     */
    public void validateSkuUniquenessForUpdate(String newSku, String oldSku) {
        if (newSku != null && !newSku.equals(oldSku)) {
            validateUniqueSku(newSku);
        }
    }

    /**
     * Validates barcode uniqueness for update.
     *
     * @param newBarcode new barcode value
     * @param oldBarcode current barcode value
     */
    public void validateBarcodeUniquenessForUpdate(String newBarcode, String oldBarcode) {
        if (newBarcode != null && !newBarcode.equals(oldBarcode)) {
            validateUniqueBarcode(newBarcode);
        }
    }

    /**
     * Validates price is not negative.
     *
     * @param price price to validate
     * @throws InvalidPriceException if invalid
     */
    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(logMsg.get("manager.price.log.invalid", price));
            throw new InvalidPriceException(price);
        }
    }

    public void validateProductDeletable(Product product) {
        boolean hasSales = salesOrderItemRepository.existsByProductId(product.getId());
        if (hasSales) {
            throw new IllegalStateException(
                    messageService.get("manager.product.error.has.sales", product.getId()));
        }

        boolean inCart = cartItemRepository.existsByProductId(product.getId());
        if (inCart) {
            throw new IllegalStateException(
                    messageService.get("manager.product.error.in.cart", product.getId()));
        }
    }

    /**
     * Validates that product is active.
     *
     * @param product product entity
     * @throws ProductInactiveException if product is not active
     */
    public void validateProductActive(Product product) {
        if (product == null) {
            throw new IllegalArgumentException(
                    messageService.get("product.validation.null")
            );
        }

        if (!product.isActive()) {
            log.warn(logMsg.get("product.validation.inactive", product.getId(), product.getSku()));
            throw new ProductInactiveException(product.getId());
        }
    }
}