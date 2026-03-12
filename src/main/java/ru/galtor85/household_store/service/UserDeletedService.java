package ru.galtor85.household_store.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletedService {

    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public void deleteUser(Long userId, Locale locale) {
        final Long finalUserId = userId;
        final Locale finalLocale = locale != null ? locale : Locale.getDefault();

        User user = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-deleted-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new EntityNotFoundException(errorMessage);
                });

        log.info(messageService.get(
                "user-deleted-service.log.user.deleting",
                user.getId(),
                user.getEmail()
        ));

        user.onRemove();
        userRepository.delete(user);

        log.info(messageService.get(
                "user-deleted-service.log.user.deleted.success",
                finalUserId
        ));
    }

    @Transactional
    public void deleteUserWithCheck(Long userId, User adminUser, Locale locale) {
        final Long finalUserId = userId;
        final User finalAdminUser = adminUser;
        final Locale finalLocale = locale != null ? locale : Locale.getDefault();

        User userToDelete = userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-deleted-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new EntityNotFoundException(errorMessage);
                });

        if (finalAdminUser.getId().equals(finalUserId)) {
            String errorMessage = messageService.get(
                    "user-deleted-service.error.user.delete.self"
            );
            log.warn(errorMessage);
            throw new SecurityException(errorMessage);
        }

        if (!finalAdminUser.getRole().canManage(userToDelete.getRole())) {
            String errorMessage = messageService.get(
                    "user-deleted-service.error.user.delete.insufficient.rights",
                    userToDelete.getRole()
            );
            log.warn(messageService.get(
                    "user-deleted-service.log.user.delete.insufficient.rights",
                    finalAdminUser.getEmail(),
                    userToDelete.getRole()
            ));
            throw new SecurityException(errorMessage);
        }

        log.info(messageService.get(
                "user-deleted-service.log.user.deleting.by.admin",
                finalAdminUser.getEmail(),
                userToDelete.getId(),
                userToDelete.getEmail()
        ));

        userToDelete.onRemove();
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
                    String errorMessage = messageService.get(
                            "user-deleted-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new EntityNotFoundException(errorMessage);
                });

        String anonymizedEmail = "deleted_" + user.getId() + "_" + user.getEmail();
        user.setActive(false);
        user.setEmail(anonymizedEmail);
        user.setMobileNumber(null);

        userRepository.save(user);

        log.info(messageService.get(
                "user-deleted-service.log.user.soft.deleted",
                finalUserId,
                anonymizedEmail
        ));
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