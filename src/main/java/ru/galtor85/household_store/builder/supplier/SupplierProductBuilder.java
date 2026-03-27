package ru.galtor85.household_store.builder.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.supplier.SupplierProductRequest;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.repository.supplier.SupplierProductRepository;

@Component
@RequiredArgsConstructor
public class SupplierProductBuilder {

    private final SupplierProductRepository supplierProductRepository;

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

    public void updateFromRequest(SupplierProduct supplierProduct,
                                  SupplierProductRequest request) {
        if (request.getSupplierPrice() != null) {
            supplierProduct.setSupplierPrice(request.getSupplierPrice());
        }
        if (request.getSupplierSku() != null) {
            supplierProduct.setSupplierSku(request.getSupplierSku());
        }
        if (request.getDeliveryTime() != null) {
            supplierProduct.setDeliveryTime(request.getDeliveryTime());
        }
        if (request.getMinOrderQuantity() != null) {
            supplierProduct.setMinOrderQuantity(request.getMinOrderQuantity());
        }
    }

    public void resetMainSupplierFlag(Long productId) {
        supplierProductRepository.findByProductId(productId)
                .forEach(sp -> {
                    sp.setMainSupplier(false);
                    supplierProductRepository.save(sp);
                });
    }
}