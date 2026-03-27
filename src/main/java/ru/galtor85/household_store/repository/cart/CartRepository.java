package ru.galtor85.household_store.repository.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.galtor85.household_store.entity.cart.Cart;
import ru.galtor85.household_store.entity.cart.CartStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    List<Cart> findByUserId(Long userId);

    List<Cart> findByStatus(CartStatus status);

    @Query("SELECT c FROM Cart c WHERE c.status = :status AND c.updatedAt < :expiryTime")
    List<Cart> findExpiredCarts(@Param("status") CartStatus status,
                                @Param("expiryTime") LocalDateTime expiryTime);

    @Modifying
    @Query("UPDATE Cart c SET c.status = :newStatus WHERE c.status = :oldStatus AND c.updatedAt < :expiryTime")
    int updateExpiredCartsStatus(@Param("oldStatus") CartStatus oldStatus,
                                 @Param("newStatus") CartStatus newStatus,
                                 @Param("expiryTime") LocalDateTime expiryTime);

    boolean existsByUserIdAndStatus(Long userId, CartStatus status);
}