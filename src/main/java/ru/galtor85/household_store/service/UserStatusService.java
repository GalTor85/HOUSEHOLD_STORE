package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.security.SecurityUserFactory;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {


    private final SecurityUserRepository securityUserRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;
    private final SecurityUserFactory securityUserFactory;

    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        // Получаем SecurityUser администратора
        SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                .orElseThrow(() -> {
                    String error = messageService.get("user-status-service.error.admin.security.not.found", adminUser.getEmail());
                    log.error(error);
                    return new AccessDeniedException(error);
                });

        // Получаем SecurityUser целевого пользователя
        SecurityUser targetSecurity = securityUserRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get("user-status-service.error.user.not.found.id", userId);
                    log.error(errorMessage);
                    return new RuntimeException(errorMessage);
                });

        // Получаем бизнес-данные целевого пользователя
        User targetUser = userSearchService.getUserById(userId, locale);

        // Проверяем, может ли админ управлять ролью целевого пользователя
        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            String errorMessage = messageService.get("user-status-service.error.status.insufficient.rights.manage", targetSecurity.getRole());
            log.warn(messageService.get("user-status-service.log.status.insufficient.rights.manage", adminUser.getEmail(), targetSecurity.getRole()));
            throw new AccessDeniedException(errorMessage);
        }

        // Нельзя деактивировать самого себя
        if (targetUser.getId().equals(adminUser.getId()) && !active) {
            String errorMessage = messageService.get("user-status-service.error.status.deactivate.self");
            log.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        // Сохраняем старый статус для логирования
        boolean oldStatus = targetSecurity.isEnabled();

        // ИСПРАВЛЕНО: передаем существующий SecurityUser в фабрику
        SecurityUser updatedSecurityUser = securityUserFactory.withUpdatedStatus(
                targetUser,
                targetSecurity,  // передаем существующий SecurityUser
                active
        );

        securityUserRepository.save(updatedSecurityUser);

        String statusText = active ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        String oldStatusText = oldStatus ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        log.info(messageService.get(
                "user-status-service.log.status.changed",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldStatusText,
                statusText
        ));

        return targetUser;
    }

    @Transactional
    public User activateUser(User adminUser, Long userId, Locale locale) {
        return toggleUserActive(adminUser, userId, true, locale);
    }

    @Transactional
    public User deactivateUser(User adminUser, Long userId, Locale locale) {
        return toggleUserActive(adminUser, userId, false, locale);
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        return securityUserRepository.findById(userId)
                .map(securityUser -> {
                    log.debug(messageService.get("user-status-service.log.status.check", userId, securityUser.isEnabled()));
                    return securityUser.isEnabled();
                })
                .orElseGet(() -> {
                    log.debug(messageService.get("user-status-service.log.status.user.not.found", userId));
                    return false;
                });
    }

    @Transactional(readOnly = true)
    public boolean canManageUserStatus(User adminUser, Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        try {
            SecurityUser adminSecurity = securityUserRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Admin not found"));

            SecurityUser targetSecurity = securityUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target not found"));

            if (userId.equals(adminUser.getId())) {
                log.debug(messageService.get("user-status-service.log.status.cannot.manage.self", adminUser.getEmail()));
                return false;
            }

            boolean canManage = adminSecurity.getRole().canManage(targetSecurity.getRole());

            log.debug(messageService.get("user-status-service.log.status.can.manage", adminUser.getEmail(), userId, canManage));

            return canManage;

        } catch (Exception e) {
            log.error(messageService.get("user-status-service.log.status.check.error", userId, e.getMessage()));
            return false;
        }
    }

    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active) {
        return toggleUserActive(adminUser, userId, active, Locale.getDefault());
    }

    @Transactional
    public User activateUser(User adminUser, Long userId) {
        return activateUser(adminUser, userId, Locale.getDefault());
    }

    @Transactional
    public User deactivateUser(User adminUser, Long userId) {
        return deactivateUser(adminUser, userId, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        return isUserActive(userId, Locale.getDefault());
    }
}