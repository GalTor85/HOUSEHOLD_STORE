package ru.galtor85.household_store.builder.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;

/**
 * Builder for supplier product entities.
 */
@Component
@RequiredArgsConstructor
public class SupplierProductBuilder {

    private final SupplierProductRepository supplierProductRepository;

    /**
     * Builds supplier product from request.
     *
     * @param request supplier product request
     * @param supplierId supplier ID
     * @param productId product ID
     * @return supplier product entity
     */
    public SupplierProduct buildFromRequest(SupplierProductRequest request,
                                            Long supplierId, Long productId) {
        return SupplierProduct.builder()
                .supplierId(supplierId)
                .productId(productId)
                .supplierPrice(request.getSupplierPrice())
                .supplierSku(request.getSupplierSku())
                .mainSupplier(request.getMainSupplier())
                .deliveryTime(request.getDeliveryTime())
                .minOrderQuantity(request.getMinOrderQuantity())
                .build();
    }

    /**
     * Resets main supplier flag for all suppliers of a product.
     *
     * @param productId product ID
     */
    public void resetMainSupplierFlag(Long productId) {
        supplierProductRepository.findByProductId(productId)
                .forEach(sp -> {
                    sp.setMainSupplier(false);
                    supplierProductRepository.save(sp);
                });
    }
}