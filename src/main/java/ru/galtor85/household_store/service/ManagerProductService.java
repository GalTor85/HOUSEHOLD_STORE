package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.Product;
import ru.galtor85.household_store.entity.ProductAttribute;
import ru.galtor85.household_store.mapper.ProductAttributeMapper;
import ru.galtor85.household_store.mapper.ProductMapper;
import ru.galtor85.household_store.repository.ProductAttributeRepository;
import ru.galtor85.household_store.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductMapper productMapper;
    private final ProductAttributeMapper attributeMapper;
    private final MessageService messageService;
    private final ProductMediaService mediaService;

    // ========== CRUD OPERATIONS ==========

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Проверка уникальности SKU
        if (productRepository.existsBySku(request.getSku())) {
            log.warn(messageService.get("manager.product.log.sku.exists", request.getSku()));
            throw new ProductAlreadyExistsException("SKU", request.getSku());
        }

        // Проверка уникальности штрих-кода (если указан)
        if (request.getBarcode() != null && !request.getBarcode().isEmpty()) {
            if (productRepository.existsByBarcode(request.getBarcode())) {
                log.warn(messageService.get("manager.product.log.barcode.exists", request.getBarcode()));
                throw new ProductAlreadyExistsException("barcode", request.getBarcode());
            }
        }

        // Используем маппер для создания сущности
        Product product = productMapper.toEntity(request, managerId);
        Product savedProduct = productRepository.save(product);

        // Добавление атрибутов (характеристик)
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            List<ProductAttribute> attributes = attributeMapper.toEntityList(request.getAttributes(), savedProduct);
            attributeRepository.saveAll(attributes);
        }

        // Создание вариантов товара (если есть)
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            createProductVariants(savedProduct, request.getVariants(), managerId, locale);
        }

        log.info(messageService.get(
                "manager.product.created.log",
                savedProduct.getSku(),
                savedProduct.getId(),
                managerId
        ));

        return productMapper.toDto(savedProduct, locale);
    }

    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateRequest request, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        // Проверка уникальности SKU при изменении
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                log.warn(messageService.get("manager.product.log.sku.exists", request.getSku()));
                throw new ProductAlreadyExistsException("SKU", request.getSku());
            }
            product.setSku(request.getSku());
        }

        // Проверка уникальности штрих-кода при изменении
        if (request.getBarcode() != null && !request.getBarcode().equals(product.getBarcode())) {
            if (productRepository.existsByBarcode(request.getBarcode())) {
                log.warn(messageService.get("manager.product.log.barcode.exists", request.getBarcode()));
                throw new ProductAlreadyExistsException("barcode", request.getBarcode());
            }
            product.setBarcode(request.getBarcode());
        }

        // Используем маппер для обновления полей
        productMapper.updateEntity(product, request);

        // Обновление атрибутов (удаляем старые, добавляем новые)
        if (request.getAttributes() != null) {
            attributeRepository.deleteByProductId(productId);
            List<ProductAttribute> attributes = attributeMapper.toEntityList(request.getAttributes(), product);
            attributeRepository.saveAll(attributes);
        }

        Product updatedProduct = productRepository.save(product);

        log.info(messageService.get(
                "manager.product.updated.log",
                updatedProduct.getId()
        ));

        return productMapper.toDto(updatedProduct, locale);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        return productMapper.toDto(product, locale);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(String name, String category, String brand,
                                        Boolean active, int page, int size,
                                        String sortBy, String sortDir, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        if (name != null || category != null || brand != null || active != null) {
            // Используем поиск с фильтрацией
            products = productRepository.searchProducts(
                    name, category, brand, null, null, pageable);

            // Дополнительная фильтрация по active в памяти (если нужно)
            if (active != null) {
                List<Product> filteredContent = products.getContent().stream()
                        .filter(p -> p.isActive() == active)
                        .collect(Collectors.toList());
                // Создаем новую страницу с отфильтрованным содержимым
                products = new org.springframework.data.domain.PageImpl<>(
                        filteredContent, pageable, filteredContent.size());
            }
        } else {
            if (active != null) {
                products = productRepository.findByActive(active, pageable);
            } else {
                products = productRepository.findAll(pageable);
            }
        }

        log.debug(messageService.get(
                "manager.products.fetched.log",
                products.getTotalElements()
        ));

        Locale finalLocale = locale;
        return products.map(product -> productMapper.toDto(product, finalLocale));
    }
    /**
     * Загрузка медиа для продукта (делегируем в ProductMediaService)
     */
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long managerId, Locale locale) {
        return mediaService.uploadMedia(productId, files, metadataJson, managerId, locale);
    }

    /**
     * Удаление медиа
     */
    public void deleteMedia(Long mediaId, Long managerId, Locale locale) {
        mediaService.deleteMedia(mediaId, managerId, locale);
    }

    /**
     * Установка главного изображения
     */
    public void setMainMedia(Long mediaId, Long managerId, Locale locale) {
        mediaService.setMainMedia(mediaId, managerId, locale);
    }

    /**
     * Получение всех медиа продукта
     */
    public List<ProductMediaDto> getProductMedia(Long productId, Locale locale) {
        return mediaService.getProductMedia(productId, locale);
    }


    // ========== INVENTORY MANAGEMENT ==========

    @Transactional
    public ProductDto adjustStock(Long productId, int quantity, String reason, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        int newQuantity = product.getQuantityInStock() + quantity;

        if (newQuantity < 0) {
            log.warn(messageService.get(
                    "manager.stock.log.invalid",
                    product.getQuantityInStock(),
                    quantity
            ));
            throw new InvalidStockOperationException(product.getQuantityInStock(), quantity);
        }

        product.setQuantityInStock(newQuantity);
        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.stock.reason.default");

        log.info(messageService.get(
                "manager.stock.adjusted.log",
                productId,
                quantity,
                newQuantity,
                reasonText
        ));

        return productMapper.toDto(updatedProduct, locale);
    }

    @Transactional
    public ProductDto updatePrice(Long productId, BigDecimal newPrice, String reason, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("manager.price.log.invalid", newPrice));
            throw new InvalidPriceException(newPrice);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);
        Product updatedProduct = productRepository.save(product);

        String reasonText = reason != null ? reason :
                messageService.get("manager.price.reason.default");

        log.info(messageService.get(
                "manager.price.updated.log",
                productId,
                oldPrice,
                newPrice,
                reasonText
        ));

        return productMapper.toDto(updatedProduct, locale);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(int threshold, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<Product> lowStockProducts = productRepository.findByQuantityInStockLessThan(threshold);

        log.debug(messageService.get(
                "manager.low.stock.fetched.log",
                lowStockProducts.size()
        ));

        Locale finalLocale = locale;
        return lowStockProducts.stream()
                .map(product -> productMapper.toDto(product, finalLocale))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto toggleProductActive(Long productId, boolean active, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", productId));
                    return new ProductNotFoundException(productId);
                });

        product.setActive(active);
        Product updatedProduct = productRepository.save(product);

        log.info(messageService.get(
                active ? "manager.product.activated.log" : "manager.product.deactivated.log",
                productId
        ));

        return productMapper.toDto(updatedProduct, locale);
    }

    // ========== VARIANT MANAGEMENT ==========

    @Transactional
    public ProductDto createProductVariant(Long parentProductId, ProductCreateRequest variantRequest,
                                           Long managerId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        Product parentProduct = productRepository.findById(parentProductId)
                .orElseThrow(() -> {
                    log.error(messageService.get("manager.product.log.not.found", parentProductId));
                    return new ProductNotFoundException(parentProductId);
                });

        if (!parentProduct.getHasVariants()) {
            parentProduct.setHasVariants(true);
            productRepository.save(parentProduct);
        }

        // Проверка уникальности SKU для варианта
        if (productRepository.existsBySku(variantRequest.getSku())) {
            log.warn(messageService.get("manager.product.log.sku.exists", variantRequest.getSku()));
            throw new ProductAlreadyExistsException("SKU", variantRequest.getSku());
        }

        try {
            // Создаем вариант товара через маппер
            Product variant = productMapper.toEntity(variantRequest, managerId);
            variant.setParentProduct(parentProduct);
            variant.setHasVariants(false);

            Product savedVariant = productRepository.save(variant);

            // Добавление атрибутов к варианту
            if (variantRequest.getAttributes() != null && !variantRequest.getAttributes().isEmpty()) {
                List<ProductAttribute> attributes = attributeMapper.toEntityList(variantRequest.getAttributes(), savedVariant);
                attributeRepository.saveAll(attributes);
            }

            log.info(messageService.get(
                    "manager.product.variant.created.log",
                    savedVariant.getSku(),
                    savedVariant.getId(),
                    parentProductId
            ));

            return productMapper.toDto(savedVariant, locale);
        } catch (Exception e) {
            log.error(messageService.get(
                    "manager.product.variant.log.error",
                    parentProductId,
                    e.getMessage()
            ));
            throw new ProductVariantException(parentProductId);
        }
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public List<ProductDto> bulkUpdatePrices(List<Long> productIds, BigDecimal newPrice, String reason, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn(messageService.get("manager.price.log.invalid", newPrice));
            throw new InvalidPriceException(newPrice);
        }

        List<Product> products = productRepository.findAllById(productIds);

        if (products.isEmpty()) {
            log.warn(messageService.get("manager.bulk.log.no.products", productIds));
            throw new BulkOperationException(productIds, 0);
        }

        for (Product product : products) {
            product.setPrice(newPrice);
        }

        List<Product> updatedProducts = productRepository.saveAll(products);

        if (updatedProducts.size() < productIds.size()) {
            log.warn(messageService.get(
                    "manager.bulk.log.partial",
                    updatedProducts.size(),
                    productIds.size()
            ));
            throw new BulkOperationException(productIds, updatedProducts.size());
        }

        String reasonText = reason != null ? reason :
                messageService.get("manager.price.reason.default");

        log.info(messageService.get(
                "manager.bulk.price.updated.log",
                updatedProducts.size(),
                reasonText
        ));

        Locale finalLocale = locale;
        return updatedProducts.stream()
                .map(product -> productMapper.toDto(product, finalLocale))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ProductDto> bulkToggleActive(List<Long> productIds, boolean active, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<Product> products = productRepository.findAllById(productIds);

        if (products.isEmpty()) {
            log.warn(messageService.get("manager.bulk.log.no.products", productIds));
            throw new BulkOperationException(productIds, 0);
        }

        for (Product product : products) {
            product.setActive(active);
        }

        List<Product> updatedProducts = productRepository.saveAll(products);

        if (updatedProducts.size() < productIds.size()) {
            log.warn(messageService.get(
                    "manager.bulk.log.partial",
                    updatedProducts.size(),
                    productIds.size()
            ));
            throw new BulkOperationException(productIds, updatedProducts.size());
        }

        log.info(messageService.get(
                active ? "manager.bulk.activated.log" : "manager.bulk.deactivated.log",
                updatedProducts.size()
        ));

        Locale finalLocale = locale;
        return updatedProducts.stream()
                .map(product -> productMapper.toDto(product, finalLocale))
                .collect(Collectors.toList());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void createProductVariants(Product parentProduct, List<ProductCreateRequest> variantRequests,
                                       Long managerId, Locale locale) {
        for (ProductCreateRequest variantRequest : variantRequests) {
            createProductVariant(parentProduct.getId(), variantRequest, managerId, locale);
        }
    }
}