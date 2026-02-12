package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

import java.time.LocalDate;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String surname;
    @Schema(description = "Дата рождения", example = "1990-01-01")
    private LocalDate birthDate;
    @Schema(description = "Возраст", example = "34")
    private Integer age;
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
        response.setBirthDate(user.getBirthDate());
        response.setAge(user.getAge());
        response.setRole(user.getRole());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt().toString());
        response.setUpdatedAt(user.getUpdatedAt().toString());
        return response;
    }
}
