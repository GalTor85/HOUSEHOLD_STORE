package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Locale finalLocale = locale;
        final Long finalUserId = userId;
        final User finalAdminUser = adminUser;

        User userToUpdate = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-status-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new RuntimeException(errorMessage);
                });

        User targetUser = userSearchService.getUserById(finalUserId, finalLocale);

        if (!finalAdminUser.getRole().canManage(targetUser.getRole())) {
            String errorMessage = messageService.get(
                    "user-status-service.error.status.insufficient.rights.manage",
                    targetUser.getRole()
            );
            log.warn(messageService.get(
                    "user-status-service.log.status.insufficient.rights.manage",
                    finalAdminUser.getEmail(),
                    targetUser.getRole()
            ));
            throw new AccessDeniedException(errorMessage);
        }

        if (userToUpdate.getId().equals(finalAdminUser.getId()) && !active) {
            String errorMessage = messageService.get(
                    "user-status-service.error.status.deactivate.self"
            );
            log.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        boolean oldStatus = userToUpdate.isActive();
        userToUpdate.setActive(active);
        User updatedUser = userRepository.save(userToUpdate);

        String statusText = active ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        String oldStatusText = oldStatus ?
                messageService.get("user-status-service.user.status.active") :
                messageService.get("user-status-service.user.status.inactive");

        log.info(messageService.get(
                "user-status-service.log.status.changed",
                finalAdminUser.getEmail(),
                updatedUser.getEmail(),
                oldStatusText,
                statusText
        ));

        return updatedUser;
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

        final Long finalUserId = userId;
        final Locale finalLocale = locale;

        return userRepository.findById(finalUserId)
                .map(user -> {
                    log.debug(messageService.get(
                            "user-status-service.log.status.check",
                            finalUserId,
                            user.isActive()
                    ));
                    return user.isActive();
                })
                .orElseGet(() -> {
                    log.debug(messageService.get(
                            "user-status-service.log.status.user.not.found",
                            finalUserId
                    ));
                    return false;
                });
    }

    @Transactional(readOnly = true)
    public boolean canManageUserStatus(User adminUser, Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Locale finalLocale = locale;
        final Long finalUserId = userId;
        final User finalAdminUser = adminUser;

        try {
            User targetUser = userSearchService.getUserById(finalUserId, finalLocale);

            if (targetUser.getId().equals(finalAdminUser.getId())) {
                log.debug(messageService.get(
                        "user-status-service.log.status.cannot.manage.self",
                        finalAdminUser.getEmail()
                ));
                return false;
            }

            boolean canManage = finalAdminUser.getRole().canManage(targetUser.getRole());

            log.debug(messageService.get(
                    "user-status-service.log.status.can.manage",
                    finalAdminUser.getEmail(),
                    targetUser.getEmail(),
                    canManage
            ));

            return canManage;

        } catch (Exception e) {
            log.error(messageService.get(
                    "user-status-service.log.status.check.error",
                    finalUserId,
                    e.getMessage()
            ));
            return false;
        }
    }

    // ========== ПЕРЕГРУЖЕННЫЕ МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========

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