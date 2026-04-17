package ru.galtor85.household_store.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.SalesOrderItem;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    boolean existsByProductId(Long productId);
}