package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.ProductPriceRule;

import java.util.List;

@Repository
public interface ProductPriceRuleRepository extends JpaRepository<ProductPriceRule, Long> {

    /**
     * Найти все правила для конкретного товара
     */
    List<ProductPriceRule> findByProductId(Long productId);

    /**
     * Найти все товары, к которым применяется конкретное правило
     */
    List<ProductPriceRule> findByPriceRuleId(Long priceRuleId);

    /**
     * Найти активные правила для товара (не исключенные)
     */
    @Query("SELECT ppr FROM ProductPriceRule ppr WHERE ppr.productId = :productId AND ppr.excluded = false")
    List<ProductPriceRule> findActiveRulesForProduct(@Param("productId") Long productId);

    /**
     * Найти исключенные правила для товара
     */
    @Query("SELECT ppr FROM ProductPriceRule ppr WHERE ppr.productId = :productId AND ppr.excluded = true")
    List<ProductPriceRule> findExcludedRulesForProduct(@Param("productId") Long productId);

    /**
     * Проверить, применяется ли правило к товару
     */
    boolean existsByProductIdAndPriceRuleId(Long productId, Long priceRuleId);

    /**
     * Получить связь по товару и правилу
     */
    @Query("SELECT ppr FROM ProductPriceRule ppr WHERE ppr.productId = :productId AND ppr.priceRuleId = :priceRuleId")
    ProductPriceRule findByProductIdAndPriceRuleId(@Param("productId") Long productId,
                                                   @Param("priceRuleId") Long priceRuleId);

    /**
     * Удалить все правила для товара
     */
    @Modifying
    @Query("DELETE FROM ProductPriceRule ppr WHERE ppr.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    /**
     * Удалить все связи с правилом
     */
    @Modifying
    @Query("DELETE FROM ProductPriceRule ppr WHERE ppr.priceRuleId = :priceRuleId")
    void deleteByPriceRuleId(@Param("priceRuleId") Long priceRuleId);

    /**
     * Получить все ID товаров, к которым применяется правило
     */
    @Query("SELECT ppr.productId FROM ProductPriceRule ppr WHERE ppr.priceRuleId = :priceRuleId")
    List<Long> findProductIdsByPriceRuleId(@Param("priceRuleId") Long priceRuleId);

    /**
     * Получить все ID правил для товара
     */
    @Query("SELECT ppr.priceRuleId FROM ProductPriceRule ppr WHERE ppr.productId = :productId")
    List<Long> findPriceRuleIdsByProductId(@Param("productId") Long productId);

    /**
     * Количество товаров, к которым применяется правило
     */
    @Query("SELECT COUNT(ppr) FROM ProductPriceRule ppr WHERE ppr.priceRuleId = :priceRuleId")
    long countProductsByPriceRuleId(@Param("priceRuleId") Long priceRuleId);

    /**
     * Массовое добавление правил для товаров
     */
    @Modifying
    @Query(value = "INSERT INTO household_schema.product_price_rules (product_id, price_rule_id, excluded) " +
            "VALUES (:productId, :priceRuleId, :excluded)", nativeQuery = true)
    void addProductPriceRule(@Param("productId") Long productId,
                             @Param("priceRuleId") Long priceRuleId,
                             @Param("excluded") boolean excluded);

    /**
     * Массовое обновление статуса исключения
     */
    @Modifying
    @Query("UPDATE ProductPriceRule ppr SET ppr.excluded = :excluded " +
            "WHERE ppr.productId = :productId AND ppr.priceRuleId = :priceRuleId")
    void updateExcludedStatus(@Param("productId") Long productId,
                              @Param("priceRuleId") Long priceRuleId,
                              @Param("excluded") boolean excluded);
}