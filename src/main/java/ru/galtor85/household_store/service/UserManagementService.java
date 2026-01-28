package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Изменить роль пользователя
     * @param adminUser пользователь-администратор, выполняющий операцию
     * @param userId ID пользователя, чью роль меняем
     * @param newRole новая роль
     */
    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        // Проверяем права администратора
        if (!adminUser.getRole().canManage(newRole)) {
            throw new AccessDeniedException("У вас недостаточно прав для назначения роли " + newRole);
        }

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя изменить свою собственную роль
        if (userToUpdate.getId().equals(adminUser.getId())) {
            throw new RuntimeException("Нельзя изменить свою собственную роль");
        }

        // Нельзя повысить роль выше своей собственной
        if (!adminUser.getRole().canManage(newRole)) {
            throw new AccessDeniedException("Нельзя назначить роль выше своей собственной");
        }

        userToUpdate.setRole(newRole);
        User updatedUser = userRepository.save(userToUpdate);

        log.info("Администратор {} изменил роль пользователя {} на {}",
                adminUser.getEmail(), updatedUser.getEmail(), newRole);

        return updatedUser;
    }

    /**
     * Получить всех пользователей (с пагинацией)
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers(User adminUser) {
        return userRepository.findAll();
    }

    /**
     * Активировать/деактивировать пользователя
     */
    @Transactional
    public User toggleUserActive(User adminUser, Long userId, boolean active) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя деактивировать самого себя
        if (userToUpdate.getId().equals(adminUser.getId()) && !active) {
            throw new RuntimeException("Нельзя деактивировать себя");
        }

        userToUpdate.setActive(active);
        return userRepository.save(userToUpdate);
    }

    /**
     * Создать нового пользователя с определенной ролью
     */
    @Transactional
    public User createUserWithRole(User adminUser, User newUser, Role role) {
        if (!adminUser.getRole().canManage(role)) {
            throw new AccessDeniedException("У вас недостаточно прав для создания пользователя с ролью " + role);
        }

        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        newUser.setRole(role);
        newUser.setActive(true);

        return userService.register(newUser); // Используем существующий UserService
    }

    /**
     * Поиск пользователей по email или имени
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String searchTerm) {
        return userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                searchTerm, searchTerm, searchTerm);
    }
}