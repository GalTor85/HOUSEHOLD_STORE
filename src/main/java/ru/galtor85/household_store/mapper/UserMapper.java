package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.security.SecurityUser;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final SecurityUserRepository securityUserRepository;

    public UserResponse build(User user) {
        if (user == null) {
            return null;
        }

        // Получаем SecurityUser для этого пользователя
        SecurityUser securityUser = securityUserRepository.findById(user.getId())
                .orElse(null);

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setSurname(user.getSurname());
        response.setMobileNumber(user.getMobileNumber());
        response.setAddress(user.getAddress());
        response.setBirthDate(user.getBirthDate());

        // Вычисляем возраст
        if (user.getBirthDate() != null) {
            response.setAge(user.getAge());
        }

        // Берем роль и статус из SecurityUser
        if (securityUser != null) {
            response.setRole(securityUser.getRole());
            response.setActive(securityUser.isEnabled());
        } else {
            response.setRole(null);
            response.setActive(false);
        }

        response.setCreator(user.getCreator());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toString());
        }

        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toString());
        }

        return response;
    }

    /**
     * Альтернативный метод, если SecurityUser уже получен
     */
    public UserResponse build(User user, SecurityUser securityUser) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setSurname(user.getSurname());
        response.setMobileNumber(user.getMobileNumber());
        response.setAddress(user.getAddress());
        response.setBirthDate(user.getBirthDate());

        if (user.getBirthDate() != null) {
            response.setAge(user.getAge());
        }

        if (securityUser != null) {
            response.setRole(securityUser.getRole());
            response.setActive(securityUser.isEnabled());
        }

        response.setCreator(user.getCreator());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toString());
        }

        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toString());
        }

        return response;
    }
}