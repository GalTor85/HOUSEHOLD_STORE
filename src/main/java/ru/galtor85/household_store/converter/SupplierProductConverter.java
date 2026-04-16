package ru.galtor85.household_store.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.response.supplier.SupplierProductDto;
import ru.galtor85.household_store.entity.product.Product;
import ru.galtor85.household_store.entity.supplier.Supplier;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.supplier.SupplierRepository;

/**
 * Converter for supplier product entities to DTOs.
 */
@Component
@RequiredArgsConstructor
public class SupplierProductConverter {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Converts supplier product entity to DTO with enriched data.
     *
     * @param supplierProduct supplier product entity
     * @param productId product ID
     * @param supplierId supplier ID
     * @return enriched supplier product DTO
     */
    public SupplierProductDto convertToDto(SupplierProduct supplierProduct, Long productId, Long supplierId) {
        Product product = productRepository.findById(productId).orElse(null);
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);

        return SupplierProductDto.builder()
                .id(supplierProduct.getId())
                .supplierId(supplierProduct.getSupplierId())
                .supplierName(supplier != null ? supplier.getName() : null)
                .productId(supplierProduct.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .supplierPrice(supplierProduct.getSupplierPrice())
                .supplierSku(supplierProduct.getSupplierSku())
                .mainSupplier(Boolean.TRUE.equals(supplierProduct.getMainSupplier()))
                .deliveryTime(supplierProduct.getDeliveryTime())
                .minOrderQuantity(supplierProduct.getMinOrderQuantity())
                .build();
    }
}