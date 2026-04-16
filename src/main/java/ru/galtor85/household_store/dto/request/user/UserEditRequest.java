package ru.galtor85.household_store.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for editing user profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for editing user profile")
public class UserEditRequest {

    @Email(message = "{user-edit-request.validation.email.invalid}")
    @Size(max = MAX_EMAIL_LENGTH, message = "{user-edit-request.validation.email.max}")
    @Schema(description = "User email address", example = "ivan@example.com")
    private String email;

    @Pattern(regexp = PHONE_PATTERN, message = "{user-edit-request.validation.mobile.invalid}")
    @Schema(description = "User mobile phone number", example = "+79863137307")
    private String mobileNumber;

    @Pattern(regexp = DATE_PATTERN, message = "{user-edit-request.validation.birthdate.invalid}")
    @Schema(description = "Birth date in format YYYY-MM-DD", example = "1990-01-01")
    private String birthDate;

    @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH, message = "{user-edit-request.validation.firstname.size}")
    @Schema(description = "User first name", example = "Ivan")
    private String firstName;

    @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH, message = "{user-edit-request.validation.lastname.size}")
    @Schema(description = "User last name", example = "Ivanov")
    private String lastName;

    @Size(max = MAX_SURNAME_LENGTH, message = "{user-edit-request.validation.surname.max}")
    @Schema(description = "User surname (patronymic)", example = "Ivanovich")
    private String surname;

    @Size(max = MAX_ADDRESS_LENGTH, message = "{user-edit-request.validation.address.max}")
    @Schema(description = "User address", example = "123 Main St, Moscow")
    private String address;
}