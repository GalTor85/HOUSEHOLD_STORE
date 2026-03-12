package ru.galtor85.household_store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEditRequest {

    @Email(message = "{user-edit-request.validation.email.invalid}")
    private String email;

    @Pattern(
            regexp = "^\\+?[0-9\\-\\s]{10,15}$",
            message = "{user-edit-request.validation.mobile.invalid}"
    )
    private String mobileNumber;

    @Schema(description = "Birth date in format YYYY-MM-DD")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "{user-edit-request.validation.birthdate.invalid}"
    )
    private String birthDate;

    @Size(min = 2, max = 50, message = "{user-edit-request.validation.firstname.size}")
    private String firstName;

    @Size(min = 2, max = 50, message = "{user-edit-request.validation.lastname.size}")
    private String lastName;

    @Size(max = 50, message = "{user-edit-request.validation.surname.size}")
    private String surname;

    @Size(max = 200, message = "{user-edit-request.validation.address.size}")
    private String address;
}