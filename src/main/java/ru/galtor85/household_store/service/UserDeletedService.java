package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedService {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final UserSearchService userSearchService;
    private final MessageService messageService;

    @Transactional
    public void deleteUser(Long userId, Locale locale) {
        final Long finalUserId = userId;
        final Locale finalLocale = locale != null ? locale : Locale.getDefault();

        User user = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.user.not.found.id", finalUserId));
                    return new UserNotFoundException(finalUserId.toString());
                });

        log.info(messageService.get("user-deleted-service.log.user.deleting", user.getId(), user.getEmail()));

        securityUserRepository.deleteByUserId(finalUserId);
        userRepository.delete(user);

        log.info(messageService.get("user-deleted-service.log.user.deleted.success", finalUserId));
    }

    @Transactional
    public void deleteUserWithCheck(Long userId, User adminUser, Locale locale) {
        final Long finalUserId = userId;
        final User finalAdminUser = adminUser;
        final Locale finalLocale = locale != null ? locale : Locale.getDefault();

        SecurityUser adminSecurity = securityUserRepository.findById(finalAdminUser.getId())
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.admin.security.not.found", finalAdminUser.getEmail()));
                    return new UserNotFoundException(finalAdminUser.getId().toString());
                });

        User userToDelete = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.user.not.found.id", finalUserId));
                    return new UserNotFoundException(finalUserId.toString());
                });

        SecurityUser targetSecurity = securityUserRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.target.security.not.found", finalUserId));
                    return new UserNotFoundException(finalUserId.toString());
                });

        if (finalAdminUser.getId().equals(finalUserId)) {
            log.warn(messageService.get("user-deleted-service.log.user.delete.self"));
            throw new UserAccessException(
                    messageService.get("user-deleted-service.error.user.delete.self")
            );
        }

        if (!adminSecurity.getRole().canManage(targetSecurity.getRole())) {
            log.warn(messageService.get(
                    "user-deleted-service.log.user.delete.insufficient.rights",
                    finalAdminUser.getEmail(),
                    targetSecurity.getRole()
            ));
            throw new UserAccessException(
                    messageService.get("user-deleted-service.error.user.delete.insufficient.rights", targetSecurity.getRole())
            );
        }

        log.info(messageService.get(
                "user-deleted-service.log.user.deleting.by.admin",
                finalAdminUser.getEmail(),
                userToDelete.getId(),
                userToDelete.getEmail()
        ));

        securityUserRepository.deleteByUserId(finalUserId);
        userRepository.delete(userToDelete);

        log.info(messageService.get(
                "user-deleted-service.log.user.deleted.success.by.admin",
                finalAdminUser.getEmail(),
                finalUserId
        ));
    }

    @Transactional
    public void softDeleteUser(Long userId, Locale locale) {
        final Long finalUserId = userId;
        final Locale finalLocale = locale != null ? locale : Locale.getDefault();

        User user = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.user.not.found.id", finalUserId));
                    return new UserNotFoundException(finalUserId.toString());
                });

        SecurityUser securityUser = securityUserRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    log.error(messageService.get("user-deleted-service.log.target.security.not.found", finalUserId));
                    return new UserNotFoundException(finalUserId.toString());
                });

        String anonymizedEmail = "deleted_" + user.getId() + "_" + user.getEmail();
        user.setEmail(anonymizedEmail);
        user.setMobileNumber(null);
        securityUser.setActive(false);

        userRepository.save(user);
        securityUserRepository.save(securityUser);

        log.info(messageService.get("user-deleted-service.log.user.soft.deleted", finalUserId, anonymizedEmail));
    }

    @Transactional
    public void deleteUser(Long userId) {
        deleteUser(userId, Locale.getDefault());
    }

    @Transactional
    public void deleteUserWithCheck(Long userId, User adminUser) {
        deleteUserWithCheck(userId, adminUser, Locale.getDefault());
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        softDeleteUser(userId, Locale.getDefault());
    }
}