package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;

    @Transactional
    public User changeUserRole(User adminUser, Long userId, Role newRole) {
        // Проверяем, что админ может управлять этой ролью
        if (!adminUser.getRole().canManage(newRole)) {
            throw new AccessDeniedException("У вас недостаточно прав для назначения роли " + newRole);
        }

        //Проверяем, что роль пользователя старше изменяемой
        if (!adminUser.getRole().canManage(userSearchService.getUserById(userId).getRole())) {
            throw new AccessDeniedException("У вас недостаточно прав для управления ролью " + userSearchService.getUserById(userId).getRole());
        }

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Нельзя изменить свою собственную роль
        if (userToUpdate.getId().equals(adminUser.getId())) {
            throw new RuntimeException("Нельзя изменить свою собственную роль");
        }

        userToUpdate.setRole(newRole);
        User updatedUser = userRepository.save(userToUpdate);

        log.info("Администратор {} изменил роль пользователя {} на {}",
                adminUser.getEmail(), updatedUser.getEmail(), newRole);

        return updatedUser;
    }

    @Transactional(readOnly = true)
    public boolean canManageRole(User adminUser, Role targetRole) {
        return adminUser.getRole().canManage(targetRole);
    }
}