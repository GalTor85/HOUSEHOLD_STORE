package ru.galtor85.household_store.repository.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.Currency;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for currency operations.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /**
     * Finds currency by code.
     *
     * @param code currency code
     * @return optional currency
     */
    Optional<Currency> findByCode(String code);

    /**
     * Finds all active currencies.
     *
     * @return list of active currencies
     */
    List<Currency> findByIsActiveTrue();

    /**
     * Finds the base currency.
     *
     * @return optional base currency
     */
    Optional<Currency> findByIsBaseTrue();

    /**
     * Checks if currency exists by code.
     *
     * @param code currency code
     * @return true if exists
     */
    boolean existsByCode(String code);

    /**
     * Checks if base currency exists.
     *
     * @return true if base currency exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Currency c WHERE c.isBase = true")
    boolean existsByIsBaseTrue();

    /**
     * Resets base currency flag for all currencies.
     */
    @Modifying
    @Query("UPDATE Currency c SET c.isBase = false")
    void resetBaseCurrency();

    @Modifying
    @Query("DELETE FROM Currency c WHERE c.deleted = true AND c.deletedAt < :threshold")
    int deleteByDeletedTrueAndDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}