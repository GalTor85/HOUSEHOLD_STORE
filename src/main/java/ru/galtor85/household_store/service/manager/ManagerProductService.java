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
import ru.galtor85.household_store.config.BusinessConfig;
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

import static ru.galtor85.household_store.constants.PaginationConstants.DESC_SORT_DIRECTION;

/**
 * Service class for managing product-related operations by managers and administrators.
 *
 * <p>This service provides comprehensive product management functionality including:</p>
 * <ul>
 *   <li>CRUD operations for products (Create, Read, Update, Delete)</li>
 *   <li>Product media management (images, videos, documents)</li>
 *   <li>Inventory management (stock adjustments, price updates)</li>
 *   <li>Product variant management (for products with variations)</li>
 *   <li>Bulk operations (price updates, status toggles)</li>
 * </ul>
 *
 * <p>All methods are secured with role-based access control (requires ADMIN or MANAGER role).</p>
 *
 * @author Household Store Team
 * @version 1.0
 * @since 1.0
 * @see Product
 * @see ProductDto
 * @see ProductValidator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerProductService {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final MessageService messageService;
    private final ProductMediaService mediaService;
    private final ProductValidator validator;
    private final ProductVariantProcessor variantProcessor;
    private final BulkProductProcessor bulkProcessor;
    private final InventoryProcessor inventoryProcessor;
    private final ProductAttributeProcessor attributeProcessor;
    private final BusinessConfig businessConfig;


    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    /**
     * Creates a new product in the system.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Validates SKU and barcode uniqueness</li>
     *   <li>Converts the request DTO to a Product entity</li>
     *   <li>Saves the product to the database</li>
     *   <li>Adds product attributes if provided</li>
     *   <li>Creates product variants if provided</li>
     *   <li>Logs the creation operation</li>
     * </ol>
     *
     * @param request   the product creation request containing all product details
     * @param managerId ID of the manager creating the product
     * @return the created product as a DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductAlreadyExistsException
     *         if SKU or barcode already exists
     */
    @Transactional
    public ProductDto createProduct(ProductCreateRequest request, Long managerId) {

        // Validate uniqueness
        validator.validateUniqueSku(request.getSku());
        validator.validateUniqueBarcode(request.getBarcode());

        // Create product entity
        Product product = productMapper.toEntity(request, managerId);
        Product savedProduct = productRepository.save(product);

        // Add product attributes if provided
        attributeProcessor.addAttributes(savedProduct, request.getAttributes());

        // Create product variants if provided
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

    /**
     * Updates an existing product.
     *
     * <p>This method performs partial updates - only fields that are provided
     * in the request will be updated. Null fields are ignored.</p>
     *
     * @param productId the ID of the product to update
     * @param request   the update request containing fields to update
     * @return the updated product as a DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product with given ID does not exist
     */
    @Transactional
    public ProductDto updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = validator.validateProductExists(productId);

        // Validate uniqueness for update
        validator.validateSkuUniquenessForUpdate(request.getSku(), product.getSku());
        validator.validateBarcodeUniquenessForUpdate(request.getBarcode(), product.getBarcode());

        // Update entity using mapper
        productMapper.updateEntity(product, request);

        // Update attributes if provided
        attributeProcessor.updateAttributes(product, request.getAttributes());


        Product updatedProduct = productRepository.save(product);

        log.info(messageService.get("manager.product.updated.log", updatedProduct.getId()));

        return productMapper.toDto(updatedProduct);
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param productId the ID of the product to retrieve
     * @return the product as a DTO with all details including media and variants
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product with given ID does not exist
     */
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId) {
        Product product = validator.validateProductExists(productId);
        return productMapper.toDto(product);
    }

    /**
     * Retrieves a paginated list of products with optional filtering.
     *
     * <p>Supports filtering by:</p>
     * <ul>
     *   <li>Product name (partial match)</li>
     *   <li>Category (exact match)</li>
     *   <li>Brand (exact match)</li>
     *   <li>Active status</li>
     * </ul>
     *
     * @param name     product name filter (partial match, case-insensitive)
     * @param category category filter (exact match)
     * @param brand    brand filter (exact match)
     * @param active   active status filter
     * @param page     page number (0-indexed)
     * @param size     number of items per page
     * @param sortBy   field to sort by
     * @param sortDir  sort direction (asc or desc)
     * @return a page of product DTOs matching the criteria
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

    // =========================================================================
    // MEDIA MANAGEMENT
    // =========================================================================

    /**
     * Uploads media files for a product.
     *
     * <p>Supports multiple file types including images, videos, and documents.
     * The first uploaded image can be set as the main product image.</p>
     *
     * @param productId    the ID of the product to attach media to
     * @param files        list of media files to upload
     * @param metadataJson JSON string containing metadata for each file
     * @param managerId    ID of the manager uploading the media
     * @return list of uploaded media DTOs
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product does not exist
     * @throws ru.galtor85.household_store.advice.exception.file.FileStorageException
     *         if file upload fails
     */
    public List<ProductMediaDto> uploadMedia(Long productId, List<MultipartFile> files,
                                             String metadataJson, Long managerId) {
        return mediaService.uploadMedia(productId, files, metadataJson, managerId);
    }

    /**
     * Deletes a media file from a product.
     *
     * <p>If the deleted media was the main image, the product's main image
     * reference will be cleared.</p>
     *
     * @param mediaId   the ID of the media to delete
     * @param managerId ID of the manager deleting the media
     * @throws ru.galtor85.household_store.advice.exception.product.ProductMediaNotFoundException
     *         if media does not exist
     */
    public void deleteMedia(Long mediaId, Long managerId) {
        mediaService.deleteMedia(mediaId, managerId);
    }

    /**
     * Sets a media file as the main image for a product.
     *
     * <p>This will automatically unset any previously set main image
     * for the same product.</p>
     *
     * @param mediaId   the ID of the media to set as main
     * @param managerId ID of the manager performing the operation
     * @throws ru.galtor85.household_store.advice.exception.product.ProductMediaNotFoundException
     *         if media does not exist
     * @throws ru.galtor85.household_store.advice.exception.product.ProductMediaException
     *         if media is not an image
     */
    public void setMainMedia(Long mediaId, Long managerId) {
        mediaService.setMainMedia(mediaId, managerId);
    }

    /**
     * Retrieves all media files associated with a product.
     *
     * @param productId the ID of the product
     * @return list of media DTOs sorted by sort order
     */
    public List<ProductMediaDto> getProductMedia(Long productId) {
        return mediaService.getProductMedia(productId);
    }

    // =========================================================================
    // INVENTORY MANAGEMENT
    // =========================================================================

    /**
     * Adjusts the stock quantity of a product.
     *
     * <p>The quantity can be positive (increase stock) or negative (decrease stock).
     * The operation will fail if the resulting stock would be negative.</p>
     *
     * @param productId the ID of the product
     * @param quantity  the amount to adjust (positive or negative)
     * @param reason    the reason for the stock adjustment
     * @return the updated product DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product does not exist
     * @throws ru.galtor85.household_store.advice.exception.stock.InvalidStockOperationException
     *         if resulting stock would be negative
     */
    @Transactional
    public ProductDto adjustStock(Long productId, int quantity, String reason) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.adjustStock(product, quantity, reason);
    }

    /**
     * Updates the price of a product.
     *
     * @param productId the ID of the product
     * @param newPrice  the new price (must be positive)
     * @param reason    the reason for the price change
     * @return the updated product DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product does not exist
     * @throws ru.galtor85.household_store.advice.exception.validation.InvalidPriceException
     *         if new price is negative or zero
     */
    @Transactional
    public ProductDto updatePrice(Long productId, BigDecimal newPrice, String reason) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.updatePrice(product, newPrice, reason);
    }

    /**
     * Retrieves a list of products with low stock levels.
     *
     * <p>A product is considered low stock when its quantity in stock
     * is below the specified threshold.</p>
     *
     * @param threshold the stock threshold (e.g., 10 means stock &lt; 10)
     * @return list of products with low stock
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(int threshold) {
        return inventoryProcessor.getLowStockProducts(threshold);
    }

    /**
     * Toggles the active status of a product.
     *
     * <p>Inactive products are not visible to customers and cannot be ordered.</p>
     *
     * @param productId the ID of the product
     * @param active    the new active status (true = active, false = inactive)
     * @return the updated product DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if product does not exist
     */
    @Transactional
    public ProductDto toggleProductActive(Long productId, boolean active) {
        Product product = validator.validateProductExists(productId);
        return inventoryProcessor.toggleActive(product, active);
    }

    // =========================================================================
    // VARIANT MANAGEMENT
    // =========================================================================

    /**
     * Creates a new product variant for an existing parent product.
     *
     * <p>Product variants are used for products that come in different
     * configurations (e.g., different colors, sizes, capacities).</p>
     *
     * @param parentProductId the ID of the parent product
     * @param variantRequest  the creation request for the variant
     * @param managerId       ID of the manager creating the variant
     * @return the created variant as a DTO
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException
     *         if parent product does not exist
     * @throws ru.galtor85.household_store.advice.exception.product.ProductVariantException
     *         if variant creation fails
     */
    @Transactional
    public ProductDto createProductVariant(Long parentProductId, ProductCreateRequest variantRequest,
                                           Long managerId) {
        return variantProcessor.createVariant(parentProductId, variantRequest, managerId);
    }

    // =========================================================================
    // BULK OPERATIONS
    // =========================================================================

    /**
     * Updates prices for multiple products in bulk.
     *
     * <p>This is useful for applying price changes across a category
     * or during sales events.</p>
     *
     * @param productIds list of product IDs to update
     * @param newPrice   the new price for all specified products
     * @param reason     the reason for the bulk price update
     * @return list of updated product DTOs
     * @throws ru.galtor85.household_store.advice.exception.stock.BulkOperationException
     *         if some products were not found
     */
    @Transactional
    public List<ProductDto> bulkUpdatePrices(List<Long> productIds, BigDecimal newPrice, String reason) {
        return bulkProcessor.bulkUpdatePrices(productIds, newPrice, reason);
    }

    /**
     * Toggles active status for multiple products in bulk.
     *
     * <p>This is useful for mass-activating or mass-deactivating products
     * during seasonal changes or inventory clearance.</p>
     *
     * @param productIds list of product IDs to update
     * @param active     the new active status for all specified products
     * @return list of updated product DTOs
     * @throws ru.galtor85.household_store.advice.exception.stock.BulkOperationException
     *         if some products were not found
     */
    @Transactional
    public List<ProductDto> bulkToggleActive(List<Long> productIds, boolean active) {
        return bulkProcessor.bulkToggleActive(productIds, active);
    }
}