package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.MediaType;
import ru.galtor85.household_store.entity.ProductMedia;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductId(Long productId);

    List<ProductMedia> findByProductIdAndMediaType(Long productId, MediaType mediaType);

    Optional<ProductMedia> findByProductIdAndIsMainTrue(Long productId);

    @Query("SELECT pm FROM ProductMedia pm WHERE pm.productId = :productId ORDER BY pm.sortOrder ASC")
    List<ProductMedia> findByProductIdOrdered(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductMedia pm SET pm.isMain = false WHERE pm.productId = :productId")
    void resetMainImage(@Param("productId") Long productId);

    boolean existsByProductIdAndIsMainTrue(Long productId);

    @Modifying
    @Query("DELETE FROM ProductMedia pm WHERE pm.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}