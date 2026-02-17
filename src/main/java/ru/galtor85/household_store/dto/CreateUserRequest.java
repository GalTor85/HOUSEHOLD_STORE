package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;

import java.time.LocalDate;




@Data
public class CreateUserRequest {

    @Email
    private String email;

    private String mobileNumber;

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

    public User toEntity(String creator) {
        return User.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .surname(surname)
                .mobileNumber(mobileNumber)
                .birthDate(birthDate)
                .role(role)
                .creator(creator)
                .active(active)
                .build();
    }
}
