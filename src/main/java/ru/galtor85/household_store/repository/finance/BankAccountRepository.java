package ru.galtor85.household_store.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.BankAccount;
import ru.galtor85.household_store.entity.finance.BankAccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BankAccount entity
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * Finds bank account by account number
     *
     * @param accountNumber account number
     * @return optional bank account
     */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * Finds all active bank accounts
     *
     * @return list of active accounts
     */
    List<BankAccount> findByActiveTrue();

    /**
     * Finds bank accounts by type
     *
     * @param accountType account type
     * @return list of accounts
     */
    List<BankAccount> findByAccountType(BankAccountType accountType);

    /**
     * Checks if account number exists
     *
     * @param accountNumber account number
     * @return true if exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Updates account balance
     *
     * @param id      account ID
     * @param balance new balance
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE BankAccount ba SET ba.balance = :balance, ba.updatedAt = CURRENT_TIMESTAMP WHERE ba.id = :id")
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);

    /**
     * Deactivates bank account
     *
     * @param id account ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE BankAccount ba SET ba.active = false, ba.updatedAt = CURRENT_TIMESTAMP WHERE ba.id = :id")
    int deactivate(@Param("id") Long id);
}