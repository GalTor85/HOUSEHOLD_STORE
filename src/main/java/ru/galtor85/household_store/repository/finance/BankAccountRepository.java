package ru.galtor85.household_store.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.BankAccount;

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
     * Checks if account number exists
     *
     * @param accountNumber account number
     * @return true if exists
     */
    boolean existsByAccountNumber(String accountNumber);

}