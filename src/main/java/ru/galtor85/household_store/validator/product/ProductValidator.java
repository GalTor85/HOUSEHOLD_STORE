package ru.galtor85.household_store.validator.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.advice.exception.product.ProductAlreadyExistsException;
import ru.galtor85.household_store.advice.exception.product.ProductNotFoundException;
import ru.galtor85.household_store.advice.exception.stock.BulkOperationException;
import ru.galtor85.household_store.advice.exception.stock.InsufficientStockException;
import ru.galtor85.household_store.advice.exception.stock.InvalidStockOperationException;
import ru.galtor85.household_store.advice.exception.validation.InvalidPriceException;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validator for product operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidator {

    private final ProductRepository productRepository;
    private final LogMessageService logMsg;

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
     * Validates stock operation won't result in negative quantity.
     *
     * @param product product entity
     * @param quantity adjustment amount
     * @throws InvalidStockOperationException if would result in negative stock
     */
    public void validateStockOperation(Product product, int quantity) {
        int newQuantity = product.getQuantityInStock() + quantity;
        if (newQuantity < 0) {
            log.warn(logMsg.get("manager.stock.log.invalid", product.getQuantityInStock(), quantity));
            throw new InvalidStockOperationException(product.getQuantityInStock(), quantity);
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
     * Validates bulk operation has at least one product.
     *
     * @param products list of found products
     * @param requestedIds list of requested product IDs
     * @throws BulkOperationException if no products found
     */
    public void validateBulkProducts(List<Product> products, List<Long> requestedIds) {
        if (products.isEmpty()) {
            log.warn(logMsg.get("manager.bulk.log.no.products", requestedIds));
            throw new BulkOperationException(requestedIds, 0);
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
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(logMsg.get("manager.order.log.insufficient.stock",
                    product.getName(), product.getQuantityInStock(), requestedQuantity));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }
}