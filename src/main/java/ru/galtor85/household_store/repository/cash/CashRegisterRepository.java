package ru.galtor85.household_store.repository.cash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.finance.CashRegister;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    Optional<CashRegister> findByRegisterNumber(String registerNumber);

    List<CashRegister> findByIsActiveTrue();

    @Query("SELECT cr FROM CashRegister cr WHERE cr.isActive = true AND cr.cashierId = :cashierId")
    Optional<CashRegister> findActiveByCashierId(@Param("cashierId") Long cashierId);
}