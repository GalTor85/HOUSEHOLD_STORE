package ru.galtor85.household_store.util.logger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.util.email.EmailMasker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger for role change operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeLogger {

    private final MessageService messageService;
    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Logs successful role change.
     *
     * @param adminUser admin who changed the role
     * @param targetUser user whose role was changed
     * @param oldRole previous role
     * @param newRole new role
     */
    public void logRoleChange(User adminUser, User targetUser, Role oldRole, Role newRole) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logMessage = messageService.get(
                "role.change.log.message",
                timestamp,
                emailMasker.maskEmail(adminUser.getEmail()),
                emailMasker.maskEmail(targetUser.getEmail()),
                messageService.get("role.name." + oldRole.name()),
                messageService.get("role.name." + newRole.name())
        );
        log.info(logMessage);
    }

    /**
     * Logs failed role change attempt.
     *
     * @param adminUser admin who attempted the change
     * @param targetUserId target user ID
     * @param attemptedRole role that was attempted
     * @param reason failure reason
     */
    public void logFailedAttempt(User adminUser, Long targetUserId, Role attemptedRole, String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        log.warn(logMsg.get(
                "role.change.failed.log",
                timestamp,
                emailMasker.maskEmail(adminUser.getEmail()),
                targetUserId,
                messageService.get("role.name." + attemptedRole.name()),
                reason
        ));
    }
}