package ru.galtor85.household_store.repository.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.promotion.PromoCode;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeAndActiveTrue(String code);

    /**
     * Increments used count for promo code.
     *
     * @param promoCodeId promo code ID
     */
    @Modifying
    @Query("UPDATE PromoCode p SET p.usedCount = p.usedCount + 1 WHERE p.id = :promoCodeId")
    void incrementUsedCount(@Param("promoCodeId") Long promoCodeId);
}