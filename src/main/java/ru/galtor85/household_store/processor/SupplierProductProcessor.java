package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.builder.SupplierProductBuilder;
import ru.galtor85.household_store.dto.SupplierProductRequest;
import ru.galtor85.household_store.dto.SupplierProductDto;
import ru.galtor85.household_store.entity.SupplierProduct;
import ru.galtor85.household_store.repository.SupplierProductRepository;
import ru.galtor85.household_store.converter.SupplierProductConverter;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.util.EntityFinder;
import ru.galtor85.household_store.validator.ValidationHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierProductProcessor {

    private final EntityFinder entityFinder;
    private final ValidationHelper validationHelper;
    private final SupplierProductRepository supplierProductRepository;
    private final SupplierProductBuilder supplierProductBuilder;
    private final SupplierProductConverter supplierProductConverter;
    private final MessageService messageService;

    @Transactional
    public SupplierProductDto addProductToSupplier(Long supplierId, Long productId,
                                                   SupplierProductRequest request,
                                                   Long managerId) {

        entityFinder.findSupplierById(supplierId);
        entityFinder.findProductById(productId);
        validationHelper.validateProductNotLinked(supplierId, productId);

        SupplierProduct supplierProduct = supplierProductBuilder.buildFromRequest(request, supplierId, productId);

        if (request.getMainSupplier()) {
            supplierProductBuilder.resetMainSupplierFlag(productId);
        }

        SupplierProduct saved = supplierProductRepository.save(supplierProduct);

        log.info(messageService.get("manager.supplier.product.added.log", productId, supplierId, managerId));

        return supplierProductConverter.convertToDto(saved, productId, supplierId);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long supplierProductId,
                                                    SupplierProductRequest request,
                                                    Long managerId) {

        SupplierProduct supplierProduct = entityFinder.findSupplierProductById(supplierProductId);
        supplierProductBuilder.updateFromRequest(supplierProduct, request);

        if (request.getMainSupplier() && !supplierProduct.getMainSupplier()) {
            supplierProductBuilder.resetMainSupplierFlag(supplierProduct.getProductId());
            supplierProduct.setMainSupplier(true);
        }

        SupplierProduct updated = supplierProductRepository.save(supplierProduct);

        log.info(messageService.get("manager.supplier.product.updated.log", supplierProductId, managerId));

        return supplierProductConverter.convertToDto(updated,
                updated.getProductId(), updated.getSupplierId());
    }

    @Transactional
    public void removeProductFromSupplier(Long supplierProductId, Long managerId) {

        SupplierProduct supplierProduct = entityFinder.findSupplierProductById(supplierProductId);
        supplierProductRepository.delete(supplierProduct);

        log.info(messageService.get("manager.supplier.product.removed.log",
                supplierProduct.getProductId(), supplierProduct.getSupplierId(), managerId));
    }
}