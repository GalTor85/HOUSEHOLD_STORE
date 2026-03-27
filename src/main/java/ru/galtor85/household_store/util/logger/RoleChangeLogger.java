package ru.galtor85.household_store.util.logger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeLogger {

    private final MessageService messageService;

    public void logRoleChange(User adminUser, User targetUser, Role oldRole, Role newRole) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String logMessage = messageService.get(
                "role.change.log.message",
                timestamp,
                adminUser.getEmail(),
                targetUser.getEmail(),
                messageService.get("role.name." + oldRole.name()),
                messageService.get("role.name." + newRole.name())
        );

        log.info(logMessage);

        // Сохраняем в аудит
        saveToAuditLog(adminUser, targetUser, oldRole, newRole);
    }

    public void logFailedAttempt(User adminUser, Long targetUserId, Role attemptedRole, String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.warn(messageService.get(
                "role.change.failed.log",
                timestamp,
                adminUser.getEmail(),
                targetUserId,
                messageService.get("role.name." + attemptedRole.name()),
                reason
        ));
    }

    private void saveToAuditLog(User adminUser, User targetUser, Role oldRole, Role newRole) {
        // TODO: Сохранить в отдельную таблицу аудита
        log.debug(messageService.get("role.change.audit.saved",
                adminUser.getId(), targetUser.getId()));
    }
}