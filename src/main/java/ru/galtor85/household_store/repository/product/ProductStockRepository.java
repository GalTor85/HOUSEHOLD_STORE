package ru.galtor85.household_store.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.product.ProductStock;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProductStock entity.
 *
 * <p>Provides methods for managing product stock across warehouses,
 * including stock queries, low stock detection, and warehouse-specific operations.</p>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    /**
     * Finds stock record for a specific product at a specific warehouse.
     *
     * @param productId product identifier
     * @param warehouseId warehouse identifier
     * @return optional stock record
     */
    Optional<ProductStock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    /**
     * Finds all stock records for a specific warehouse.
     *
     * @param warehouseId warehouse identifier
     * @return list of stock records
     */
    List<ProductStock> findByWarehouseId(Long warehouseId);

    /**
     * Finds paginated stock records for a specific warehouse.
     *
     * @param warehouseId warehouse identifier
     * @param pageable pagination information
     * @return page of stock records
     */
    Page<ProductStock> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * Finds all stock records for a specific product across all warehouses.
     *
     * @param productId product identifier
     * @return list of stock records
     */
    List<ProductStock> findByProductId(Long productId);

    /**
     * Finds low stock items in a warehouse (quantity < minStockLevel).
     *
     * @param warehouseId warehouse identifier
     * @return list of low stock records
     */
    @Query("SELECT ps FROM ProductStock ps WHERE ps.warehouseId = :warehouseId AND ps.quantity < ps.minStockLevel")
    List<ProductStock> findLowStockItems(@Param("warehouseId") Long warehouseId);

    /**
     * Gets total stock quantity for a product across all warehouses.
     *
     * @param productId product identifier
     * @return total stock quantity
     */
    @Query("SELECT SUM(ps.quantity) FROM ProductStock ps WHERE ps.productId = :productId")
    Integer getTotalStockForProduct(@Param("productId") Long productId);

    /**
     * Finds stock records for multiple warehouses.
     *
     * @param warehouseIds list of warehouse identifiers
     * @return list of stock records
     */
    @Query("SELECT ps FROM ProductStock ps WHERE ps.warehouseId IN :warehouseIds")
    List<ProductStock> findByWarehouseIds(@Param("warehouseIds") List<Long> warehouseIds);

    /**
     * Gets stock summary grouped by warehouse.
     *
     * @return list of [warehouseId, totalQuantity] objects
     */
    @Query("SELECT ps.warehouseId, SUM(ps.quantity) as totalQuantity " +
            "FROM ProductStock ps GROUP BY ps.warehouseId")
    List<Object[]> getStockSummaryByWarehouse();

    /**
     * Gets total available stock for a product across all warehouses.
     * Available = SUM(quantity) - SUM(reservedQuantity)
     *
     * @param productId product identifier
     * @return total available quantity
     */
    @Query("SELECT COALESCE(SUM(ps.quantity) - COALESCE(SUM(ps.reservedQuantity), 0), 0) " +
            "FROM ProductStock ps WHERE ps.productId = :productId")
    Integer getAvailableStockForProduct(@Param("productId") Long productId);

    /**
     * Gets total available stock for a product across visible warehouses only.
     * Only includes warehouses marked as visible for sale.
     *
     * @param productId product identifier
     * @return total available quantity from visible warehouses
     */
    @Query("SELECT COALESCE(SUM(ps.quantity) - COALESCE(SUM(ps.reservedQuantity), 0), 0) " +
            "FROM ProductStock ps " +
            "JOIN Warehouse w ON ps.warehouseId = w.id " +
            "WHERE ps.productId = :productId AND w.isVisibleForSale = true")
    Integer getAvailableStockForCustomer(@Param("productId") Long productId);

    /**
     * Gets stock details by warehouse for a product (without visibility filter).
     *
     * @param productId product identifier
     * @return list of [warehouseId, quantity, reservedQuantity, availableQuantity]
     */
    @Query("SELECT ps.warehouseId, ps.quantity, ps.reservedQuantity, " +
            "COALESCE(ps.quantity - ps.reservedQuantity, ps.quantity) as available " +
            "FROM ProductStock ps " +
            "WHERE ps.productId = :productId")
    List<Object[]> getStockByWarehouseForProduct(@Param("productId") Long productId);

    /**
     * Gets stock details by warehouse for a product with visibility flag.
     *
     * @param productId product identifier
     * @return list of [warehouseId, quantity, reservedQuantity, availableQuantity, isVisibleForSale]
     */
    @Query("SELECT ps.warehouseId, ps.quantity, ps.reservedQuantity, " +
            "COALESCE(ps.quantity - ps.reservedQuantity, ps.quantity) as available, w.isVisibleForSale " +
            "FROM ProductStock ps " +
            "JOIN Warehouse w ON ps.warehouseId = w.id " +
            "WHERE ps.productId = :productId")
    List<Object[]> getStockByWarehouseWithVisibility(@Param("productId") Long productId);

    /**
     * Searches stock in a warehouse by product name or SKU.
     *
     * @param warehouseId warehouse identifier
     * @param searchTerm search term (product name or SKU)
     * @param pageable pagination information
     * @return page of stock records
     */
    @Query("SELECT ps FROM ProductStock ps JOIN Product p ON ps.productId = p.id " +
            "WHERE ps.warehouseId = :warehouseId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ProductStock> searchOnWarehouse(@Param("warehouseId") Long warehouseId,
                                         @Param("searchTerm") String searchTerm,
                                         Pageable pageable);

    /**
     * Gets stock records for a warehouse with sorting by product fields.
     *
     * @param warehouseId warehouse identifier
     * @param sortBy field to sort by (productName, category, sku, brand, price)
     * @param sortDir sort direction (asc/desc)
     * @param pageable pagination information
     * @return page of stock records
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
