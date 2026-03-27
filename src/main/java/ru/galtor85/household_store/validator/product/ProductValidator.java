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
import ru.galtor85.household_store.service.i18n.MessageService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductValidator {

    private final ProductRepository productRepository;
    private final MessageService messageService;

    public Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    public void validateUniqueSku(String sku) {
        if (productRepository.existsBySku(sku)) {
            log.warn(messageService.get("manager.product.log.sku.exists", sku));
            throw new ProductAlreadyExistsException("SKU", sku);
        }
    }

    public void validateUniqueBarcode(String barcode) {
        if (barcode != null && !barcode.isEmpty() && productRepository.existsByBarcode(barcode)) {
            log.warn(messageService.get("manager.product.log.barcode.exists", barcode));
            throw new ProductAlreadyExistsException("barcode", barcode);
        }
    }

    public void validateSkuUniquenessForUpdate(String newSku, String oldSku) {
        if (newSku != null && !newSku.equals(oldSku)) {
            validateUniqueSku(newSku);
        }
    }

    public void validateBarcodeUniquenessForUpdate(String newBarcode, String oldBarcode) {
        if (newBarcode != null && !newBarcode.equals(oldBarcode)) {
            validateUniqueBarcode(newBarcode);
        }
    }

    public void validateStockOperation(Product product, int quantity) {
        int newQuantity = product.getQuantityInStock() + quantity;
        if (newQuantity < 0) {
            log.warn(messageService.get(
                    "manager.stock.log.invalid",
                    product.getQuantityInStock(),
                    quantity
            ));
            throw new InvalidStockOperationException(product.getQuantityInStock(), quantity);
        }
    }

    public void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("manager.price.log.invalid", price));
            throw new InvalidPriceException(price);
        }
    }

    public void validateBulkProducts(List<Product> products, List<Long> requestedIds) {
        if (products.isEmpty()) {
            log.warn(messageService.get("manager.bulk.log.no.products", requestedIds));
            throw new BulkOperationException(requestedIds, 0);
        }
    }

    /**
     * Поиск продукта по ID и проверка наличия достаточного количества
     */
    public Product findAndValidateProduct(Long productId, int requestedQuantity) {
        Product product = findProductById(productId);
        validateStockAvailability(product, requestedQuantity);
        return product;
    }

    /**
     * Поиск продукта по ID
     */
    public Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });
    }

    /**
     * Проверка наличия достаточного количества товара
     */
    public void validateStockAvailability(Product product, int requestedQuantity) {
        if (product.getQuantityInStock() < requestedQuantity) {
            log.warn(messageService.get(
                    "manager.order.log.insufficient.stock",
                    product.getName(),
                    product.getQuantityInStock(),
                    requestedQuantity
            ));
            throw new InsufficientStockException(product.getName(), product.getQuantityInStock());
        }
    }
}