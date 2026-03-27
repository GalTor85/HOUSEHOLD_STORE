package ru.galtor85.household_store.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.order.SalesOrderItem;

import java.util.List;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    /**
     * Находит все позиции заказа
     */
    List<SalesOrderItem> findBySalesOrderId(Long salesOrderId);

    /**
     * Находит все позиции с товаром
     */
    List<SalesOrderItem> findByProductId(Long productId);

    /**
     * Удаляет все позиции заказа
     */
    @Modifying
    @Query("DELETE FROM SalesOrderItem soi WHERE soi.salesOrder.id = :orderId")
    void deleteBySalesOrderId(@Param("orderId") Long orderId);
}