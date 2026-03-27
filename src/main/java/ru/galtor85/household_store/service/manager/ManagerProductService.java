package ru.galtor85.household_store.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.dto.request.product.ProductUpdateRequest;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.mapper.product.ProductMapper;
import ru.galtor85.household_store.processor.bulk.BulkProductProcessor;
import ru.galtor85.household_store.processor.inventory.InventoryProcessor;
import ru.galtor85.household_store.processor.product.ProductAttributeProcessor;
import ru.galtor85.household_store.processor.product.ProductVariantProcessor;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.service.product.ProductMediaService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final MessageService messageService;
    private final ProductMediaService mediaService;

    // Валидаторы
    private final ProductValidator validator;

    // Процессоры
    private final ProductVariantProcessor variantProcessor;
    private final BulkProductProcessor bulkProcessor;
    private final InventoryProcessor inventoryProcessor;
    private final ProductAttributeProcessor attributeProcessor;

    // ========== CRUD OPERATIONS ==========

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long managerId) {
        // Валидация
        validator.validateUniqueSku(request.getSku());
        validator.validateUniqueBarcode(request.getBarcode());

        // Создание продукта
        Product product = productMapper.toEntity(request, managerId);
        Product savedProduct = productRepository.save(product);

        // Добавление атрибутов
        attributeProcessor.addAttributes(savedProduct, request.getAttributes());

        // Создание вариантов
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            variantProcessor.createVariants(savedProduct, request.getVariants(), managerId);
        }

        log.info(messageService.get(
                "manager.product.created.log",
                savedProduct.getSku(),
                savedProduct.getId(),
                managerId
        ));

        return productMapper.toDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = validator.validateProductExists(productId);

        // Валидация уникальности
        validator.validateSkuUniquenessForUpdate(request.getSku(), product.getSku());
        validator.validateBarcodeUniquenessForUpdate(request.getBarcode(), product.getBarcode());

        // Обновление через маппер
        productMapper.updateEntity(product, request);

        // Обновление атрибутов
        attributeProcessor.updateAttributes(product, request.getAttributes());

        Product updatedProduct = productRepository.save(product);

        log.info(messageService.get("manager.product.updated.log", updatedProduct.getId()));

        return productMapper.toDto(updatedProduct);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = validator.validateProductExists(productId);
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(String name, String category, String brand,
                                        Boolean active, int page, int size,
                                        String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        if (name != null || category != null || brand != null || active != null) {
            products = productRepository.searchProducts(
                    name, category, brand, null, null, pageable);

            if (active != null) {
                List<Product> filteredContent = products.getContent().stream()
                        .filter(p -> p.isActive() == active)
                        .collect(Collectors.toList());
                products = new PageImpl<>(filteredContent, pageable, filteredContent.size());
            }
        } else {
            if (active != null) {
                products = productRepository.findByActive(active, pageable);
            } else {
                products = productRepository.findAll(pageable);
            }
        }

        log.debug(messageService.get("manager.products.fetched.log", products.getTotalElements()));

        return products.map(productMapper::toDto);
    }

    // ========== MEDIA MANAGEMENT ==========

    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long managerId) {
        return mediaService.uploadMedia(productId, files, metadataJson, managerId);
    }

    public void deleteMedia(Long mediaId, Long managerId) {
        mediaService.deleteMedia(mediaId, managerId);
    }

    public void setMainMedia(Long mediaId, Long managerId) {
        mediaService.setMainMedia(mediaId, managerId);
    }

    public List<ProductMediaDto> getProductMedia(Long productId) {
        return mediaService.getProductMedia(productId);
    }

    // ========== INVENTORY MANAGEMENT ==========

    @Transactional
    public ProductDto adjustStock(Long productId, int quantity, String reason) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.adjustStock(product, quantity, reason);
    }

    @Transactional
    public ProductDto updatePrice(Long productId, BigDecimal newPrice, String reason) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.updatePrice(product, newPrice, reason);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(int threshold) {
        return inventoryProcessor.getLowStockProducts(threshold);
    }

    @Transactional
    public ProductDto toggleProductActive(Long productId, boolean active) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.toggleActive(product, active);
    }

    // ========== VARIANT MANAGEMENT ==========

    @Transactional
    public ProductDto createProductVariant(Long parentProductId, ProductCreateRequest variantRequest,
                                           Long managerId) {
        return variantProcessor.createVariant(parentProductId, variantRequest, managerId);
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public List<ProductDto> bulkUpdatePrices(List<Long> productIds, BigDecimal newPrice, String reason) {
        return bulkProcessor.bulkUpdatePrices(productIds, newPrice, reason);
    }

    @Transactional
    public List<ProductDto> bulkToggleActive(List<Long> productIds, boolean active) {
        return bulkProcessor.bulkToggleActive(productIds, active);
    }
}