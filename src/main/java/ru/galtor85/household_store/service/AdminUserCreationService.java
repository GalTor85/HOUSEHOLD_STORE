package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCreationService {

    private final UserRepository userRepository;
    private final UserService userService; // или PasswordEncoder напрямую

    @Transactional
    public  User createUserWithRole(User adminUser, User newUser, Role role) {
        // Проверяем права администратора
        if (!adminUser.getRole().canManage(role)) {
            throw new AccessDeniedException(
                    "У вас недостаточно прав для создания пользователя с ролью " + role);
        }

        // Проверяем уникальность email
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        if (newUser.getBirthDate() == null) {
            newUser.setBirthDate(LocalDate.now().minusYears(18));  // Устанавливаем дату рождения по умолчанию
        }
        newUser.setRole(role);
        newUser.setActive(true);

        // Создаем пользователя через существующий сервис
        User createdUser = userService.register(newUser);

        log.info("Администратор {} создал пользователя {} с ролью {}",
                adminUser.getEmail(), createdUser.getEmail(), role);

        return createdUser;
    }

    @Transactional
    public User createUserWithGeneratedPassword(User adminUser, User newUser, Role role) {
        // Альтернативный метод с генерацией пароля
        String generatedPassword = generateRandomPassword();
        newUser.setPassword(generatedPassword);

        User createdUser = createUserWithRole(adminUser, newUser, role);

        // TODO: Отправить email с паролем
        log.info("Сгенерирован пароль для пользователя {}: {}",
                createdUser.getEmail(), generatedPassword);

        return createdUser;
    }

    private String generateRandomPassword() {
        // Логика генерации пароля
        return "TempPass123!"; // Временная реализация
    }
}