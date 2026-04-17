package ru.galtor85.household_store.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.warehouse.CellType;
import ru.galtor85.household_store.entity.warehouse.StorageCell;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storage cell operations.
 */
@Repository
public interface StorageCellRepository extends JpaRepository<StorageCell, Long> {

    /**
     * Finds all cells in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @return list of storage cells
     */
    List<StorageCell> findByWarehouseId(Long warehouseId);

    /**
     * Finds cell by code and warehouse ID.
     *
     * @param code cell code
     * @param warehouseId warehouse ID
     * @return optional storage cell
     */
    Optional<StorageCell> findByCodeAndWarehouseId(String code, Long warehouseId);

    /**
     * Finds cells containing a specific product in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @param productId product ID
     * @return list of storage cells
     */
    List<StorageCell> findByWarehouseIdAndCurrentProductId(Long warehouseId, Long productId);

    /**
     * Finds available cells of specific type in a warehouse.
     *
     * @param warehouseId warehouse ID
     * @param cellType cell type
     * @return list of available storage cells
     */
    @Query("SELECT sc FROM StorageCell sc WHERE " +
            "sc.warehouse.id = :warehouseId AND " +
            "sc.cellType = :cellType AND " +
            "sc.isOccupied = false AND " +
            "sc.isActive = true")
    List<StorageCell> findAvailableCellsByType(@Param("warehouseId") Long warehouseId,
                                               @Param("cellType") CellType cellType);

    @Query("SELECT COUNT(sc) > 0 FROM StorageCell sc WHERE sc.warehouse.id = :warehouseId")
    boolean hasCellsByWarehouseId(@Param("warehouseId") Long warehouseId);

    boolean existsByCodeAndWarehouseId(String code, Long warehouseId);
}