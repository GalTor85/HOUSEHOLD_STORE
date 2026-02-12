package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.galtor85.household_store.entity.Role;

import java.time.LocalDate;


@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @Schema(description = "Дата рождения")
    private LocalDate birthDate;

    private String firstName;
    private String lastName;
    private String surname;
    private Role role; //= Role.USER? TODO
    private boolean active = true;
}
