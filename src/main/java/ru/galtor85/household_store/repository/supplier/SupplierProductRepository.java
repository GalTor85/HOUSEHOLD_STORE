package ru.galtor85.household_store.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.supplier.SupplierProduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    List<SupplierProduct> findBySupplierId(Long supplierId);

    List<SupplierProduct> findByProductId(Long productId);

    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);

    @Query("SELECT sp FROM SupplierProduct sp WHERE sp.productId = :productId ORDER BY sp.supplierPrice ASC")
    List<SupplierProduct> findCheapestSuppliersForProduct(@Param("productId") Long productId);

    @Query("SELECT sp FROM SupplierProduct sp WHERE sp.supplierId = :supplierId AND sp.mainSupplier = true")
    List<SupplierProduct> findMainProductsBySupplier(@Param("supplierId") Long supplierId);

    @Modifying
    @Query("UPDATE SupplierProduct sp SET sp.mainSupplier = false WHERE sp.supplierId = :supplierId")
    void resetMainSupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT MIN(sp.supplierPrice) FROM SupplierProduct sp WHERE sp.productId = :productId")
    BigDecimal findMinPriceForProduct(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM SupplierProduct sp WHERE sp.supplierId = :supplierId")
    void deleteBySupplierId(@Param("supplierId") Long supplierId);
}