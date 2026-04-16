package ru.galtor85.household_store.repository.price;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.promotion.PriceRule;
import ru.galtor85.household_store.entity.user.UserType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {

    @Query("SELECT pr FROM PriceRule pr JOIN pr.applicableUserTypes ut " +
            "WHERE pr.active = true AND ut = :userType " +
            "AND (pr.startDate IS NULL OR pr.startDate <= :now) " +
            "AND (pr.endDate IS NULL OR pr.endDate >= :now)")
    List<PriceRule> findActiveRulesForUserType(@Param("userType") UserType userType,
                                               @Param("now") LocalDateTime now);
}