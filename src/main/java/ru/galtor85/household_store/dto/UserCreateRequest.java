package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @Email(message = "{user-create-request.validation.email.invalid}")
    private String email;

    @Pattern(
            regexp = "^\\+?[0-9\\-\\s]{10,15}$",
            message = "{user-create-request.validation.mobile.invalid}"
    )
    private String mobileNumber;

    @NotBlank(message = "{user-create-request.validation.password.empty}")
    @Size(min = 6, message = "{user-create-request.validation.password.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$",
            message = "{user-create-request.validation.password.invalid}"
    )
    private String password;

    @Schema(description = "Birth date in format YYYY-MM-DD")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "{user-create-request.validation.birthdate.invalid}"
    )
    private String birthDate;

    @NotBlank(message = "{user-create-request.validation.firstname.empty}")
    @Size(min = 2, max = 50, message = "{user-create-request.validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{user-create-request.validation.lastname.empty}")
    @Size(min = 2, max = 50, message = "{user-create-request.validation.lastname.size}")
    private String lastName;

    @Size(max = 50, message = "{user-create-request.validation.surname.size}")
    private String surname;

    @Size(max = 200, message = "{user-create-request.validation.address.size}")
    private String address;

    @Schema(description = "User role (default: USER)")
    private Role role;

    @Schema(description = "User active status (default: true)")
    private Boolean active;
}