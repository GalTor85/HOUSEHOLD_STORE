package ru.galtor85.household_store.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;

import java.util.List;
import java.util.Optional;

/**
 * Repository for supplier product operations.
 */
@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    /**
     * Finds all supplier product links for a product.
     *
     * @param productId product ID
     * @return list of supplier products
     */
    List<SupplierProduct> findByProductId(Long productId);

    /**
     * Finds supplier product link by supplier and product.
     *
     * @param supplierId supplier ID
     * @param productId product ID
     * @return optional supplier product
     */
    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);
}