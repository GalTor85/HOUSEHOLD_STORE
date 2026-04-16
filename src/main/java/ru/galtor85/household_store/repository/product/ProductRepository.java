package ru.galtor85.household_store.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.product.Product;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Boolean existsBySku(String sku);

    Boolean  existsByBarcode(String barcode);

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByQuantityInStockLessThan(int threshold);

    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "p.active = true")
    Page<Product> searchProducts(@Param("name") String name,
                                 @Param("category") String category,
                                 @Param("brand") String brand,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 Pageable pageable);

    /**
     * Finds products by category with pagination.
     *
     * @param category category name
     * @param pageable pagination information
     * @return page of active products in category
     */
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.active = true")
    List<String> findAllCategories();

}