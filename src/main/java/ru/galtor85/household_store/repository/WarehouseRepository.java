package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.Warehouse;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    Optional<Warehouse> findByBarcode(String barcode);

    boolean existsByCode(String code);

    boolean existsByBarcode(String barcode);

    Page<Warehouse> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT w FROM Warehouse w WHERE " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(w.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "w.barcode LIKE CONCAT('%', :search, '%')")
    Page<Warehouse> searchWarehouses(@Param("search") String search, Pageable pageable);
}