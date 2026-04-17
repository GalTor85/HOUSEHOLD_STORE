package ru.galtor85.household_store.service.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.galtor85.household_store.config.BusinessConfig;
import ru.galtor85.household_store.dto.request.product.ProductCreateRequest;
import ru.galtor85.household_store.dto.request.product.ProductUpdateRequest;
import ru.galtor85.household_store.dto.response.product.ProductDto;
import ru.galtor85.household_store.dto.response.product.ProductMediaDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.product.ProductMedia;
import ru.galtor85.household_store.entity.product.ProductStock;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.mapper.product.ProductMapper;
import ru.galtor85.household_store.processor.inventory.InventoryProcessor;
import ru.galtor85.household_store.processor.product.ProductAttributeProcessor;
import ru.galtor85.household_store.processor.product.ProductVariantProcessor;
import ru.galtor85.household_store.repository.product.ProductAttributeRepository;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;
import ru.galtor85.household_store.service.file.FileStorageService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.product.ProductMediaService;
import ru.galtor85.household_store.validator.product.ProductValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.PaginationConstants.DESC_SORT_DIRECTION;

/**
 * Service for managing product-related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductMediaService mediaService;
    private final ProductValidator validator;
    private final ProductVariantProcessor variantProcessor;
    private final InventoryProcessor inventoryProcessor;
    private final ProductAttributeProcessor attributeProcessor;
    private final BusinessConfig businessConfig;
    private final LogMessageService logMsg;
    private final SupplierProductRepository supplierProductRepository;
    private final FileStorageService fileStorageService;
    private final ProductMediaRepository mediaRepository;
    private final ProductStockRepository stockRepository;
    private final ProductAttributeRepository attributeRepository;




    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    /**
     * Creates a new product.
     *
     * @param request   product creation request
     * @param managerId manager ID
     * @return created product DTO
     */
    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long managerId) {
        validator.validateUniqueSku(request.getSku());
        validator.validateUniqueBarcode(request.getBarcode());

        Product product = productMapper.toEntity(request, managerId);
        Product savedProduct = productRepository.save(product);

        attributeProcessor.addAttributes(savedProduct, request.getAttributes());

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            variantProcessor.createVariants(savedProduct, request.getVariants(), managerId);
        }

        log.info(logMsg.get("manager.product.created.log",
                savedProduct.getSku(), savedProduct.getId(), managerId));

        return productMapper.toDto(savedProduct);
    }

    /**
     * Updates an existing product.
     *
     * @param productId product ID
     * @param request   update request
     * @return updated product DTO
     */
    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = validator.validateProductExists(productId);

        validator.validateSkuUniquenessForUpdate(request.getSku(), product.getSku());
        validator.validateBarcodeUniquenessForUpdate(request.getBarcode(), product.getBarcode());

        productMapper.updateEntity(product, request);
        attributeProcessor.updateAttributes(product, request.getAttributes());

        Product updatedProduct = productRepository.save(product);

        log.info(logMsg.get("manager.product.updated.log", updatedProduct.getId()));

        return productMapper.toDto(updatedProduct);
    }

    /**
     * Retrieves a product by ID.
     *
     * @param productId product ID
     * @return product DTO
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = validator.validateProductExists(productId);
        return productMapper.toDto(product);
    }

    /**
     * Permanently deletes a product and all related data.
     *
     * @param productId product ID
     * @param deletedBy ID of user performing deletion
     */
    @Transactional
    public void deleteProduct(Long productId, Long deletedBy) {
        Product product = validator.validateProductExists(productId);

        // Check if product can be deleted (no sales history)
        validator.validateProductDeletable(product);

        // Delete all related data
        deleteProductRelations(product, deletedBy);

        // Finally delete the product
        productRepository.delete(product);

        log.info(logMsg.get("manager.product.deleted.log",
                product.getSku(), product.getId(), deletedBy));
    }

    private void deleteProductRelations(Product product, Long deletedBy) {
        // 1. Delete media files from disk and database
        List<ProductMedia> mediaList = mediaRepository.findByProductId(product.getId());
        for (ProductMedia media : mediaList) {
            fileStorageService.deleteFile(media.getFilePath(), product.getId());
            log.debug(logMsg.get("manager.product.media.deleted",
                    media.getId(), product.getId(), deletedBy));
        }
        mediaRepository.deleteAll(mediaList);

        // 2. Delete product stock records
        List<ProductStock> stocks = stockRepository.findByProductId(product.getId());
        stockRepository.deleteAll(stocks);
        log.debug(logMsg.get("manager.product.stocks.deleted",
                stocks.size(), product.getId(), deletedBy));

        // 3. Delete attributes
        attributeRepository.deleteByProductId(product.getId());
        log.debug(logMsg.get("manager.product.attributes.deleted",
                product.getId(), deletedBy));

        // 4. Delete supplier product links
        List<SupplierProduct> supplierProducts = supplierProductRepository.findByProductId(product.getId());
        supplierProductRepository.deleteAll(supplierProducts);
        log.debug(logMsg.get("manager.product.supplier.links.deleted",
                supplierProducts.size(), product.getId(), deletedBy));

        // 5. Delete variants recursively
        for (Product variant : product.getVariants()) {
            deleteProductRelations(variant, deletedBy);
            productRepository.delete(variant);
            log.debug(logMsg.get("manager.product.variant.deleted",
                    variant.getId(), product.getId(), deletedBy));
        }
    }

    /**
     * Retrieves paginated products with optional filters.
     *
     * @param name     name filter (partial match)
     * @param category category filter
     * @param brand    brand filter
     * @param active   active status filter
     * @param page     page number
     * @param size     page size
     * @param sortBy   sort field
     * @param sortDir  sort direction
     * @return page of product DTOs
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(String name, String category, String brand,
                                        Boolean active, Integer page, Integer size,
                                        String sortBy, String sortDir) {

        int effectivePage = page != null ? page : businessConfig.getPagination().getDefaultPage();
        int effectiveSize = size != null ? size : businessConfig.getPagination().getDefaultSize();

        Sort sort = sortDir.equalsIgnoreCase(DESC_SORT_DIRECTION) ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, sort);

        Page<Product> products;

        if (name != null || category != null || brand != null || active != null) {
            products = productRepository.searchProducts(name, category, brand, null, null, pageable);

            if (active != null) {
                List<Product> filteredContent = products.getContent().stream()
                        .filter(p -> p.isActive() == active)
                        .collect(Collectors.toList());
                products = new PageImpl<>(filteredContent, pageable, filteredContent.size());
            }
        } else {
            products = productRepository.findAll(pageable);
        }

        log.debug(logMsg.get("manager.products.fetched.log", products.getTotalElements()));

        return products.map(productMapper::toDto);
    }

    // =========================================================================
    // MEDIA MANAGEMENT
    // =========================================================================

    /**
     * Uploads media files for a product.
     *
     * @param productId    product ID
     * @param files        media files
     * @param metadataJson JSON metadata
     * @param managerId    manager ID
     * @return list of uploaded media DTOs
     */
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long managerId) {
        return mediaService.uploadMedia(productId, files, metadataJson, managerId);
    }

    /**
     * Deletes a media file.
     *
     * @param mediaId   media ID
     * @param managerId manager ID
     */
    public void deleteMedia(Long mediaId, Long managerId) {
        mediaService.deleteMedia(mediaId, managerId);
    }

    /**
     * Sets a media file as main product image.
     *
     * @param mediaId   media ID
     * @param managerId manager ID
     */
    public void setMainMedia(Long mediaId, Long managerId) {
        mediaService.setMainMedia(mediaId, managerId);
    }

    // =========================================================================
    // INVENTORY MANAGEMENT
    // =========================================================================

    /**
     * Updates product price.
     *
     * @param productId product ID
     * @param newPrice  new price
     * @param reason    reason for change
     * @return updated product DTO
     */
    @Transactional
    public ProductDto updatePrice(Long productId, BigDecimal newPrice, String reason) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.updatePrice(product, newPrice, reason);
    }

    /**
     * Retrieves products with low stock.
     *
     * @param threshold stock threshold
     * @return list of low stock products
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(int threshold) {
        return inventoryProcessor.getLowStockProducts(threshold);
    }

    /**
     * Toggles product active status.
     *
     * @param productId product ID
     * @param active    new active status
     * @return updated product DTO
     */
    @Transactional
    public ProductDto toggleProductActive(Long productId, boolean active) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.toggleActive(product, active);
    }
 }