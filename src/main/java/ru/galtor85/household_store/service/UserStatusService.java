package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;

    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        //Проверяем, что роль пользователя старше изменяемой
        if (!adminUser.getRole().canManage(userSearchService.getUserById(userId).getRole())) {
            throw new AccessDeniedException("У вас недостаточно прав для управления ролью " + userSearchService.getUserById(userId).getRole());
        }

        // Нельзя деактивировать самого себя
        if (userToUpdate.getId().equals(adminUser.getId()) && !active) {
            throw new RuntimeException("Нельзя деактивировать себя");
        }

        userToUpdate.setActive(active);

        log.info("Администратор {} изменил статус пользователя {} на {}",
                adminUser.getEmail(), userToUpdate.getEmail(), active ? "активен" : "неактивен");

        return userRepository.save(userToUpdate);
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        return userRepository.findById(userId)
                .map(User::isActive)
                .orElse(false);
    }
}