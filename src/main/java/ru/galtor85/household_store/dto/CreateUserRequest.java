package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.galtor85.household_store.entity.Role;


@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    private String firstName;
    private String lastName;
    private String surname;
    private Role role = Role.USER;
    private boolean active = true;
}
