package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

/**
 * Processor for changing user account status (active/inactive).
 *
 * <p>Handles the activation and deactivation of user accounts.
 * This processor updates the SecurityUser entity and logs the status change
 * for audit purposes.</p>
 *
 * <p><b>Note:</b> This processor assumes that all permission checks
 * (admin rights, not self-deactivating, etc.) have already been performed
 * by the calling service.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusChangeProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final SecurityUserFactory securityUserFactory;
    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Changes the active status of a user account.
     *
     * <p>Updates the SecurityUser entity with the new status and persists
     * the change to the database. Logs the status change with both old
     * and new status values for audit trail.</p>
     *
     * @param targetUser     the domain user entity whose status is being changed
     * @param targetSecurity the security user entity to update
     * @param active         true to activate, false to deactivate
     * @param adminUser      the admin user performing the status change
     */
    @Transactional
    public void changeStatus(User targetUser,
                             SecurityUser targetSecurity,
                             boolean active,
                             User adminUser) {

        String maskedAdminEmail = emailMasker.maskEmail(adminUser.getEmail());
        String maskedTargetEmail = emailMasker.maskEmail(targetUser.getEmail());

        log.debug(logMsg.get("user.status.change.start",
                maskedTargetEmail,
                active ? "activate" : "deactivate",
                maskedAdminEmail));

        boolean oldStatus = targetSecurity.isEnabled();

        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedStatus(
                targetSecurity,
                active
        );

        securityUserRepository.save(updatedSecurityUser);

        logStatusChange(adminUser.getEmail(), targetUser.getEmail(), oldStatus, active);

        log.debug(logMsg.get("user.status.change.complete",
                maskedTargetEmail,
                active ? "activated" : "deactivated"));

    }

    /**
     * Logs the status change with localized status names.
     *
     * @param adminEmail the email of the admin performing the change
     * @param targetEmail the email of the user whose status is changed
     * @param oldStatus the previous active status
     * @param newStatus the new active status
     */
    private void logStatusChange(String adminEmail,
                                 String targetEmail,
                                 boolean oldStatus,
                                 boolean newStatus) {

        String oldStatusText = oldStatus
                ? messageService.get("user-status-service.user.status.active")
                : messageService.get("user-status-service.user.status.inactive");

        String newStatusText = newStatus
                ? messageService.get("user-status-service.user.status.active")
                : messageService.get("user-status-service.user.status.inactive");

        log.info(logMsg.get(
                "user-status-service.log.status.changed",
                emailMasker.maskEmail(adminEmail),
                emailMasker.maskEmail(targetEmail),
                oldStatusText,
                newStatusText
        ));
    }
}