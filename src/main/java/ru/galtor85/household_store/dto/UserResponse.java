package ru.galtor85.household_store.dto;

import lombok.Data;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String surname;
    private Role role;
    private boolean active;
    private String createdAt;
    private String updatedAt;

    public static UserResponse fromEntity(  User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setSurname(user.getSurname());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt().toString());
        response.setUpdatedAt(user.getUpdatedAt().toString());
        return response;
    }
}
