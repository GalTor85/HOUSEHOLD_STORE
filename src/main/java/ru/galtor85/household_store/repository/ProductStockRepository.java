package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.ProductStock;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    Optional<ProductStock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<ProductStock> findByWarehouseId(Long warehouseId);

    Page<ProductStock> findByWarehouseId(Long warehouseId, Pageable pageable);

    List<ProductStock> findByProductId(Long productId);

    @Query("SELECT ps FROM ProductStock ps WHERE ps.warehouseId = :warehouseId AND ps.quantity < ps.minStockLevel")
    List<ProductStock> findLowStockItems(@Param("warehouseId") Long warehouseId);

    @Query("SELECT SUM(ps.quantity) FROM ProductStock ps WHERE ps.productId = :productId")
    Integer getTotalStockForProduct(@Param("productId") Long productId);

    @Query("SELECT ps FROM ProductStock ps WHERE ps.warehouseId IN :warehouseIds")
    List<ProductStock> findByWarehouseIds(@Param("warehouseIds") List<Long> warehouseIds);

    @Query("SELECT ps.warehouseId, SUM(ps.quantity) as totalQuantity " +
            "FROM ProductStock ps GROUP BY ps.warehouseId")
    List<Object[]> getStockSummaryByWarehouse();


    @Query("SELECT ps FROM ProductStock ps JOIN Product p ON ps.productId = p.id " +
            "WHERE ps.warehouseId = :warehouseId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ProductStock> searchOnWarehouse(@Param("warehouseId") Long warehouseId,
                                         @Param("searchTerm") String searchTerm,
                                         Pageable pageable);

       /**
     * Получение остатков с сортировкой по полям продукта
     */
    @Query("SELECT ps FROM ProductStock ps JOIN Product p ON ps.productId = p.id " +
            "WHERE ps.warehouseId = :warehouseId " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'productName' AND :sortDir = 'asc' THEN p.name END ASC, " +
            "CASE WHEN :sortBy = 'productName' AND :sortDir = 'desc' THEN p.name END DESC, " +
            "CASE WHEN :sortBy = 'category' AND :sortDir = 'asc' THEN p.category END ASC, " +
            "CASE WHEN :sortBy = 'category' AND :sortDir = 'desc' THEN p.category END DESC, " +
            "CASE WHEN :sortBy = 'sku' AND :sortDir = 'asc' THEN p.sku END ASC, " +
            "CASE WHEN :sortBy = 'sku' AND :sortDir = 'desc' THEN p.sku END DESC, " +
            "CASE WHEN :sortBy = 'brand' AND :sortDir = 'asc' THEN p.brand END ASC, " +
            "CASE WHEN :sortBy = 'brand' AND :sortDir = 'desc' THEN p.brand END DESC, " +
            "CASE WHEN :sortBy = 'price' AND :sortDir = 'asc' THEN p.price END ASC, " +
            "CASE WHEN :sortBy = 'price' AND :sortDir = 'desc' THEN p.price END DESC, " +
            "ps.productId ASC")
    Page<ProductStock> findByWarehouseIdWithSort(@Param("warehouseId") Long warehouseId,
                                                 @Param("sortBy") String sortBy,
                                                 @Param("sortDir") String sortDir,
                                                 Pageable pageable);
}

