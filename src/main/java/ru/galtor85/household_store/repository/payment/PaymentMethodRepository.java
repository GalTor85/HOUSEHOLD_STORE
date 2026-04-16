package ru.galtor85.household_store.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.payment.PaymentMethod;
import ru.galtor85.household_store.entity.payment.PaymentProvider;

import java.util.List;

/**
 * Repository for PaymentMethod entity
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * Finds payment methods by provider
     *
     * @param provider payment provider
     * @return list of payment methods
     */
    List<PaymentMethod> findByProvider(PaymentProvider provider);
}