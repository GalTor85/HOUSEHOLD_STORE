package ru.galtor85.household_store.processor.supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.supplier.SupplierProductBuilder;
import ru.galtor85.household_store.converter.SupplierProductConverter;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.entity.EntityFinder;
import ru.galtor85.household_store.validator.common.ValidationHelper;

/**
 * Processor for managing supplier-product relationships.
 *
 * <p>Handles the business logic for linking products to suppliers,
 * updating supplier-specific product information, and removing these links.
 * This processor operates at the service layer, coordinating between
 * repositories, builders, converters, and validators.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Adding products to a supplier's catalog with supplier-specific pricing and SKU</li>
 *   <li>Updating existing supplier-product relationships</li>
 *   <li>Removing products from a supplier's catalog</li>
 *   <li>Managing the "main supplier" flag for products</li>
 * </ul>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierProductProcessor {

    private final EntityFinder entityFinder;
    private final ValidationHelper validationHelper;
    private final SupplierProductRepository supplierProductRepository;
    private final SupplierProductBuilder supplierProductBuilder;
    private final SupplierProductConverter supplierProductConverter;
    private final LogMessageService logMsg;

    /**
     * Adds a product to a supplier's catalog.
     *
     * <p>Creates a new relationship between the specified supplier and product.
     * If the product is marked as the main supplier for this product,
     * all other suppliers' "main supplier" flags for this product will be reset.</p>
     *
     * @param supplierId the ID of the supplier
     * @param productId  the ID of the product to add
     * @param request    the request containing supplier-specific product data (price, SKU, etc.)
     * @param managerId  the ID of the manager performing the operation
     * @return DTO representing the created supplier-product relationship
     * @throws ru.galtor85.household_store.advice.exception.supplier.SupplierNotFoundException if supplier not found
     * @throws ru.galtor85.household_store.advice.exception.product.ProductNotFoundException if product not found
     * @throws ru.galtor85.household_store.advice.exception.supplier.SupplierProductAlreadyExistsException if link already exists
     */
    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId,
                                                   Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId) {
        log.debug(logMsg.get("supplier.product.processor.add.start", productId, supplierId, managerId));

        entityFinder.findSupplierById(supplierId);
        entityFinder.findProductById(productId);
        validationHelper.validateProductNotLinked(supplierId, productId);

        SupplierProduct supplierProduct = supplierProductBuilder.buildFromRequest(request, supplierId, productId);

        if (Boolean.TRUE.equals(request.getMainSupplier())) {
            supplierProductBuilder.resetMainSupplierFlag(productId);
        }

        SupplierProduct saved = supplierProductRepository.save(supplierProduct);

        log.info(logMsg.get("manager.supplier.product.added.log", productId, supplierId, managerId));

        return supplierProductConverter.convertToDto(saved, productId, supplierId);
    }
}