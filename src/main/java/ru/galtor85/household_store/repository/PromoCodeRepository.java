package ru.galtor85.household_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.PromoCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeAndActiveTrue(String code);
    List<PromoCode> findByActiveTrueAndEndDateAfter(LocalDateTime date);
}