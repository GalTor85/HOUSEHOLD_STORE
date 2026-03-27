package ru.galtor85.household_store.processor.delete;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class HardDeleteProcessor {

    private final SecurityUserRepository securityUserRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional
    public void deleteUser(User user, Long userId) {
        log.info(messageService.get("user-deleted-service.log.user.deleting",
                user.getId(), user.getEmail()));

        // Сначала удаляем security запись, потом пользователя
        securityUserRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info(messageService.get("user-deleted-service.log.user.deleted.success", userId));
    }

    @Transactional
    public void deleteUserByAdmin(User userToDelete, User adminUser, Long userId) {
        log.info(messageService.get(
                "user-deleted-service.log.user.deleting.by.admin",
                adminUser.getEmail(),
                userToDelete.getId(),
                userToDelete.getEmail()
        ));

        securityUserRepository.deleteByUserId(userId);
        userRepository.delete(userToDelete);

        log.info(messageService.get(
                "user-deleted-service.log.user.deleted.success.by.admin",
                adminUser.getEmail(),
                userId
        ));
    }
}