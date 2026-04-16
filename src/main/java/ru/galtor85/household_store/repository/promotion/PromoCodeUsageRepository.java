package ru.galtor85.household_store.repository.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.promotion.PromoCodeUsage;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    /**
     * Counts the number of times a promo code has been used by a specific user.
     *
     * @param promoCodeId promo code ID
     * @param userId user ID
     * @return count of usage
     */
    long countByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

}