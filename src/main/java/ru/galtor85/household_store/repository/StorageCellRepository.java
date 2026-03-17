package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.CellType;
import ru.galtor85.household_store.entity.StorageCell;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageCellRepository extends JpaRepository<StorageCell, Long> {

    List<StorageCell> findByWarehouseId(Long warehouseId);

    Page<StorageCell> findByWarehouseId(Long warehouseId, Pageable pageable);

    Optional<StorageCell> findByCodeAndWarehouseId(String code, Long warehouseId);

    Optional<StorageCell> findByBarcode(String barcode);

    List<StorageCell> findByWarehouseIdAndIsOccupiedTrue(Long warehouseId);

    List<StorageCell> findByWarehouseIdAndCurrentProductId(Long warehouseId, Long productId);

    @Query("SELECT sc FROM StorageCell sc WHERE " +
            "sc.warehouse.id = :warehouseId AND " +
            "sc.cellType = :cellType AND " +
            "sc.isOccupied = false AND " +
            "sc.isActive = true")
    List<StorageCell> findAvailableCellsByType(@Param("warehouseId") Long warehouseId,
                                               @Param("cellType") CellType cellType);

    @Query("SELECT COUNT(sc) FROM StorageCell sc WHERE sc.warehouse.id = :warehouseId AND sc.isOccupied = false")
    long countAvailableCells(@Param("warehouseId") Long warehouseId);

    @Modifying
    @Query("UPDATE StorageCell sc SET sc.isOccupied = false, sc.currentProductId = null, sc.currentQuantity = 0 " +
            "WHERE sc.id = :cellId")
    void clearCell(@Param("cellId") Long cellId);
}