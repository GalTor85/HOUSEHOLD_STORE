package ru.galtor85.household_store.validator.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.cart.CartItemRepository;
import ru.galtor85.household_store.repository.order.SalesOrderItemRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.stock.StockService;

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
    private final StockService stockService;

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

    /**
     * Finds product by ID and validates stock availability.
     *
     * @param productId product ID
     * @param requestedQuantity requested quantity
     * @return product entity
     * @throws ProductNotFoundException if not found
     * @throws InsufficientStockException if insufficient stock
     */
    public Product findAndValidateProduct(Long productId, int requestedQuantity) {
        Product product = findProductById(productId);
        validateStockAvailability(product, requestedQuantity);
        return product;
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
     * Finds product by ID.
     *
     * @param productId product ID
     * @return product entity
     * @throws ProductNotFoundException if not found
     */
    public Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(logMsg.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Validates sufficient stock availability.
     *
     * @param product product entity
     * @param requestedQuantity requested quantity
     * @throws InsufficientStockException if insufficient stock
     */
    public void validateStockAvailability(Product product, int requestedQuantity) {
        Integer totalStock = stockService.getTotalStockForProduct(product.getId());
        int availableStock = totalStock != null ? totalStock : 0;

        if (availableStock < requestedQuantity) {
            log.warn(logMsg.get("manager.order.log.insufficient.stock",
                    product.getName(), availableStock, requestedQuantity));
            throw new InsufficientStockException(product.getName(), availableStock);
        }
    }
}