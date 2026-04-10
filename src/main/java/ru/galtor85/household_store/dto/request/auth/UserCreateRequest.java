package ru.galtor85.household_store.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.galtor85.household_store.entity.user.Role;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @Email(message = "{user-create-request.validation.email.invalid}")
    @Schema(description = "User email", example = "ivan@example.com")
    private String email;

    @Pattern(
            regexp = PHONE_PATTERN,
            message = "{user-create-request.validation.mobile.invalid}"
    )
    @Schema(description = "User mobile number", example = "+79863137307")
    private String mobileNumber;

    @NotBlank(message = "{user-create-request.validation.password.empty}")
    @Pattern(
            regexp = PASSWORD_PATTERN,
            message = "{user-create-request.validation.password.invalid}"
    )
    @Schema(description = "User password", example = "Password123!")
    private String password;

    @Schema(description = "Birth date in format YYYY-MM-DD", example = "1990-01-01")
    @Pattern(
            regexp = DATE_PATTERN,
            message = "{user-create-request.validation.birthdate.invalid}"
    )
    private String birthDate;

    @NotBlank(message = "{user-create-request.validation.firstname.empty}")
    @Schema(description = "User first name", example = "Иван")
    private String firstName;

    @NotBlank(message = "{user-create-request.validation.lastname.empty}")
    @Schema(description = "User last name", example = "Иванов")
    private String lastName;

    @Schema(description = "User surname (patronymic)", example = "Иванович")
    private String surname;

    @Schema(description = "User address", example = "г. Москва, ул. Тверская, д. 1")
    private String address;

    @Schema(description = "User role (default: USER)", example = "USER")
    private Role role;

    @Schema(description = "User active status (default: true)", example = "true")
    private Boolean active;
}