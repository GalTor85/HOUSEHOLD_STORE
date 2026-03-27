package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;

@Component
@RequiredArgsConstructor
public class SupplierProductConverter {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    public SupplierProductDto convertToDto(SupplierProduct sp, Long productId,
                                           Long supplierId) {
        Product product = productRepository.findById(productId).orElse(null);
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);

        return SupplierProductDto.builder()
                .id(sp.getId())
                .supplierId(sp.getSupplierId())
                .supplierName(supplier != null ? supplier.getName() : null)
                .productId(sp.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .supplierPrice(sp.getSupplierPrice())
                .supplierSku(sp.getSupplierSku())
                .mainSupplier(Boolean.TRUE.equals(sp.getMainSupplier()))
                .deliveryTime(sp.getDeliveryTime())
                .minOrderQuantity(sp.getMinOrderQuantity())
                .build();
    }
}