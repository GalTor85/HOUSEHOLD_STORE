package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.CategoryWarehouse;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryWarehouseRepository extends JpaRepository<CategoryWarehouse, Long> {

    Optional<CategoryWarehouse> findByCategory(String category);

    List<CategoryWarehouse> findByWarehouseId(Long warehouseId);

    @Query("SELECT cw FROM CategoryWarehouse cw WHERE cw.category = :category ORDER BY cw.priority ASC")
    List<CategoryWarehouse> findByCategoryOrderedByPriority(@Param("category") String category);

    @Query("SELECT cw.warehouseId FROM CategoryWarehouse cw WHERE cw.category = :category AND cw.isDefault = true")
    Optional<Long> findDefaultWarehouseByCategory(@Param("category") String category);

    // ДОБАВЛЯЕМ ЭТОТ МЕТОД
    boolean existsByCategory(String category);

    void deleteByCategory(String category);



    @Query("SELECT cw FROM CategoryWarehouse cw WHERE cw.category IN :categories")
    List<CategoryWarehouse> findByCategoryIn(@Param("categories") List<String> categories);

    @Query("SELECT cw.category FROM CategoryWarehouse cw WHERE cw.warehouseId = :warehouseId")
    List<String> findCategoriesByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Modifying
    @Query("DELETE FROM CategoryWarehouse cw WHERE cw.category IN :categories")
    void deleteByCategoryIn(@Param("categories") List<String> categories);
}