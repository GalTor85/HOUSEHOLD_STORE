package ru.galtor85.household_store.repository.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.supplier.Supplier;

/**
 * Repository for supplier operations.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Searches suppliers by name and status with pagination.
     *
     * @param name supplier name filter
     * @param status supplier status filter
     * @param pageable pagination information
     * @return page of suppliers
     */
    @Query(value = "SELECT * FROM household_schema.suppliers s WHERE " +
            "(:name IS NULL OR CAST(s.name AS TEXT) ILIKE CONCAT('%', CAST(:name AS TEXT), '%')) AND " +
            "(:status IS NULL OR s.status = CAST(:status AS TEXT)) " +
            "ORDER BY s.name",
            countQuery = "SELECT COUNT(*) FROM household_schema.suppliers s WHERE " +
                    "(:name IS NULL OR CAST(s.name AS TEXT) ILIKE CONCAT('%', CAST(:name AS TEXT), '%')) AND " +
                    "(:status IS NULL OR s.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    Page<Supplier> searchSuppliersNative(@Param("name") String name,
                                         @Param("status") String status,
                                         Pageable pageable);

    /**
     * Checks if supplier exists by INN.
     *
     * @param inn supplier INN
     * @return true if exists
     */
    boolean existsByInn(String inn);

    /**
     * Checks if supplier exists by email.
     *
     * @param email supplier email
     * @return true if exists
     */
    boolean existsByEmail(String email);
}