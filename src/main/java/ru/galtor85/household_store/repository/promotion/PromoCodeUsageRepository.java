package ru.galtor85.household_store.repository.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.promotion.PromoCodeUsage;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    /**
     * Подсчет использований промокода пользователем
     */
    long countByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

    /**
     * Проверка, использовал ли пользователь промокод
     */
    boolean existsByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

    /**
     * Получить все использования промокода пользователем
     */
    List<PromoCodeUsage> findByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

    /**
     * Получить последнее использование промокода пользователем
     */
    Optional<PromoCodeUsage> findFirstByPromoCodeIdAndUserIdOrderByUsedAtDesc(Long promoCodeId, Long userId);

    /**
     * Получить все использования промокода
     */
    List<PromoCodeUsage> findByPromoCodeId(Long promoCodeId);

    /**
     * Получить все использования пользователем
     */
    List<PromoCodeUsage> findByUserId(Long userId);

    /**
     * Удалить все использования промокода
     */
    void deleteByPromoCodeId(Long promoCodeId);

    /**
     * Удалить все использования пользователя
     */
    void deleteByUserId(Long userId);

    /**
     * Подсчет использований промокода за период
     */
    @Query("SELECT COUNT(pcu) FROM PromoCodeUsage pcu " +
            "WHERE pcu.promoCodeId = :promoCodeId " +
            "AND pcu.usedAt BETWEEN :startDate AND :endDate")
    long countByPromoCodeIdAndDateRange(@Param("promoCodeId") Long promoCodeId,
                                        @Param("startDate") java.time.LocalDateTime startDate,
                                        @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Получить статистику использования промокода по дням
     */
    @Query("SELECT DATE(pcu.usedAt), COUNT(pcu) FROM PromoCodeUsage pcu " +
            "WHERE pcu.promoCodeId = :promoCodeId " +
            "GROUP BY DATE(pcu.usedAt) ORDER BY DATE(pcu.usedAt)")
    List<Object[]> getDailyUsageStats(@Param("promoCodeId") Long promoCodeId);
}