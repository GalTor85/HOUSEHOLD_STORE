package ru.galtor85.household_store.processor.delete;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.cart.CartStatus;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.cart.CartRepository;
import ru.galtor85.household_store.repository.order.SalesOrderRepository;
import ru.galtor85.household_store.repository.payment.PaymentTransactionRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.LogMessageService;

/**
 * Processor for hard deletion of users.
 * Falls back to soft delete if user has dependencies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HardDeleteProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final UserRepository userRepository;
    private final LogMessageService logMsg;
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SoftDeleteProcessor softDeleteProcessor;
    private final CartRepository cartRepository;

    /**
     * Deletes a user by admin. Always performs hard delete.
     *
     * @param userToDelete the user to delete
     * @param adminUser    the admin performing the deletion
     * @param userId       the user ID
     */
    @Transactional
    public void deleteUserByAdmin(User userToDelete, User adminUser, Long userId) {

        if (isUserDeletable(userId)) {
            hardDelete(userToDelete, userId);
        } else {
            softDelete(userToDelete, userId);
        }

        log.info(logMsg.get(
                "user-deleted-service.log.user.deleting.by.admin",
                adminUser.getEmail(),
                userToDelete.getId(),
                userToDelete.getEmail()
        ));

        hardDelete(userToDelete, userId);

        log.info(logMsg.get(
                "user-deleted-service.log.user.deleted.success.by.admin",
                adminUser.getEmail(),
                userId
        ));
    }

    /**
     * Checks if user can be hard deleted (no dependencies).
     *
     * @param userId the user ID
     * @return true if hard delete is possible
     */
    private boolean isUserDeletable(Long userId) {
        long orderCount = salesOrderRepository.countByUserId(userId);
        long paymentCount = paymentTransactionRepository.countByCreatedBy(userId);
        boolean hasActiveCart = cartRepository.existsByUserIdAndStatus(userId, CartStatus.ACTIVE);

        return orderCount == 0 && paymentCount == 0 && !hasActiveCart;
    }

    /**
     * Performs hard delete of user.
     *
     * @param user   the user to delete
     * @param userId the user ID
     */
    private void hardDelete(User user, Long userId) {
        log.info(logMsg.get("user-deleted-service.log.user.hard.deleting", userId));

        securityUserRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info(logMsg.get("user-deleted-service.log.user.hard.deleted", userId));
    }

    /**
     * Performs soft delete of user.
     *
     * @param user   the user to delete
     * @param userId the user ID
     */
    private void softDelete(User user, Long userId) {
        log.info(logMsg.get("user-deleted-service.log.user.soft.deleting", userId));

        softDeleteProcessor.softDeleteUser(user, userId);

        log.info(logMsg.get("user-deleted-service.log.user.soft.deleted", userId));
    }
}