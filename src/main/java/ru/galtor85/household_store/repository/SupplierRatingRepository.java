package ru.galtor85.household_store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.SupplierRating;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {

    List<SupplierRating> findBySupplierId(Long supplierId);

    Page<SupplierRating> findBySupplierId(Long supplierId, Pageable pageable);

    Optional<SupplierRating> findBySupplierIdAndUserId(Long supplierId, Long userId);

    @Query("SELECT AVG(sr.rating) FROM SupplierRating sr WHERE sr.supplierId = :supplierId")
    Double getAverageRating(@Param("supplierId") Long supplierId);

    @Query("SELECT COUNT(sr) FROM SupplierRating sr WHERE sr.supplierId = :supplierId")
    Integer getRatingCount(@Param("supplierId") Long supplierId);

    boolean existsBySupplierIdAndUserId(Long supplierId, Long userId);

    @Query("SELECT sr FROM SupplierRating sr WHERE sr.verified = true AND sr.supplierId = :supplierId")
    List<SupplierRating> findVerifiedBySupplierId(@Param("supplierId") Long supplierId);

    @Modifying
    @Query("DELETE FROM SupplierRating sr WHERE sr.supplierId = :supplierId")
    void deleteBySupplierId(@Param("supplierId") Long supplierId);
}