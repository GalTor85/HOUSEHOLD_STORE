package ru.galtor85.household_store.repository.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.Currency;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    // =========================================================================
    // БАЗОВЫЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Находит валюту по коду
     */
    Optional<Currency> findByCode(String code);

    /**
     * Находит все активные валюты
     */
    List<Currency> findByIsActiveTrue();

    /**
     * Находит базовую валюту
     */
    Optional<Currency> findByIsBaseTrue();

    /**
     * Проверяет существование валюты по коду
     */
    boolean existsByCode(String code);

    /**
     * Проверяет, существует ли базовая валюта
     */
    @Query("SELECT COUNT(c) > 0 FROM Currency c WHERE c.isBase = true")
    boolean existsByIsBaseTrue();

    // =========================================================================
    // ОБНОВЛЕНИЯ
    // =========================================================================

    /**
     * Сбрасывает флаг isBase у всех валют
     */
    @Modifying
    @Query("UPDATE Currency c SET c.isBase = false")
    void resetBaseCurrency();

    /**
     * Обновляет курс обмена валюты
     */
    @Modifying
    @Query("UPDATE Currency c SET c.exchangeRate = :rate, c.updatedAt = CURRENT_TIMESTAMP WHERE c.code = :code")
    int updateExchangeRate(@Param("code") String code, @Param("rate") java.math.BigDecimal rate);

    /**
     * Активирует/деактивирует валюту
     */
    @Modifying
    @Query("UPDATE Currency c SET c.isActive = :active, c.updatedAt = CURRENT_TIMESTAMP WHERE c.code = :code")
    int updateActiveStatus(@Param("code") String code, @Param("active") Boolean active);

    // =========================================================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =========================================================================

    /**
     * Получает количество активных валют
     */
    @Query("SELECT COUNT(c) FROM Currency c WHERE c.isActive = true")
    long countActiveCurrencies();

    /**
     * Получает список кодов всех валют
     */
    @Query("SELECT c.code FROM Currency c WHERE c.isActive = true")
    List<String> findAllActiveCodes();

    /**
     * Получает курсы обмена для всех активных валют
     */
    @Query("SELECT c.code, c.exchangeRate FROM Currency c WHERE c.isActive = true")
    List<Object[]> findAllActiveExchangeRates();

    // =========================================================================
    // ПРОВЕРКИ
    // =========================================================================

    /**
     * Проверяет, можно ли деактивировать валюту (не используется в счетах)
     */
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.currency = :code")
    boolean isCurrencyUsedInInvoices(@Param("code") String code);

    /**
     * Проверяет, можно ли деактивировать валюту (не используется в кассовых операциях)
     */
    @Query("SELECT COUNT(ct) > 0 FROM CashTransaction ct WHERE ct.currency = :code")
    boolean isCurrencyUsedInCashTransactions(@Param("code") String code);
}