package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.Supplier;
import ru.galtor85.household_store.entity.SupplierStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findByInn(String inn);

    List<Supplier> findByStatus(SupplierStatus status);

    Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM household_schema.suppliers s WHERE " +
            "(:name IS NULL OR CAST(s.name AS TEXT) ILIKE CONCAT('%', CAST(:name AS TEXT), '%')) AND " +
            "(:status IS NULL OR s.status = CAST(:status AS TEXT)) " +
            "ORDER BY s.name ASC",
            countQuery = "SELECT COUNT(*) FROM household_schema.suppliers s WHERE " +
                    "(:name IS NULL OR CAST(s.name AS TEXT) ILIKE CONCAT('%', CAST(:name AS TEXT), '%')) AND " +
                    "(:status IS NULL OR s.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    Page<Supplier> searchSuppliersNative(@Param("name") String name,
                                         @Param("status") String status,
                                         Pageable pageable);

    // Можно оставить старый метод для совместимости, если нужно
    @Query("SELECT s FROM Supplier s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:status IS NULL OR s.status = :status)")
    Page<Supplier> searchSuppliers(@Param("name") String name,
                                   @Param("status") SupplierStatus status,
                                   Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.rating >= :minRating")
    List<Supplier> findByMinRating(@Param("minRating") Double minRating);

    boolean existsByInn(String inn);

    boolean existsByEmail(String email);
}