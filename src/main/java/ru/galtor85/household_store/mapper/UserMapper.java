package ru.galtor85.household_store.mapper;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.UserResponse;
import ru.galtor85.household_store.entity.User;

@Component
public class UserMapper {
    public UserResponse build(User user) {
        if (user == null) {
            return null;  // Если пользователь null, вернем null
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
        response.setAge(user.getAge());
        response.setRole(user.getRole());
        response.setCreator(user.getCreator());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt().toString());
        response.setUpdatedAt(user.getUpdatedAt().toString());
        return response;
    }
}