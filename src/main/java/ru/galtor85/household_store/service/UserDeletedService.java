package ru.galtor85.household_store.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.UserRepository;

@Slf4j
@Service
@AllArgsConstructor
public class UserDeletedService {

    private final UserRepository userRepository;

       /**
       * Удалить пользователя с определенной ролью
       */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Логирование перед удалением
        log.info("Удаление пользователя: ID={}, Email={}", user.getId(), user.getEmail());

        // Вызов @PreRemove метода в сущности (если есть)
        user.OnRemove();

        // Удаляем пользователя
        userRepository.delete(user);
    }
}
