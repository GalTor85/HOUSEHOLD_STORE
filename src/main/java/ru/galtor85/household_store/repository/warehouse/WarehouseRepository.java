package ru.galtor85.household_store.repository.warehouse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.warehouse.Warehouse;

import java.util.List;

/**
 * Repository for Warehouse entity.
 *
 * <p>Provides methods for warehouse CRUD operations, search by code/barcode,
 * and filtering by visibility for customer sales.</p>
 *
 * @author G@LTor85
 
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /**
     * Checks if warehouse with given code exists.
     *
     * @param code warehouse code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Checks if warehouse with given barcode exists.
     *
     * @param barcode warehouse barcode
     * @return true if exists
     */
    boolean existsByBarcode(String barcode);

    /**
     * Searches warehouses by name, code or barcode.
     *
     * @param search search term
     * @param pageable pagination information
     * @return page of matching warehouses
     */
    @Query("SELECT w FROM Warehouse w WHERE " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(w.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "w.barcode LIKE CONCAT('%', :search, '%')")
    Page<Warehouse> searchWarehouses(@Param("search") String search, Pageable pageable);

    /**
     * Finds all warehouses with optional visibility filter.
     * When includeInvisible is false, only visible warehouses are returned.
     * When includeInvisible is true, all warehouses are returned.
     *
     * @param includeInvisible whether to include invisible warehouses
     * @return list of warehouses based on visibility filter
     */
    @Query("SELECT w FROM Warehouse w WHERE (:includeInvisible = true OR w.isVisibleForSale = true)")
    List<Warehouse> findWarehousesForSale(@Param("includeInvisible") boolean includeInvisible);
}