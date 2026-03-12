package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatePasswordRequest {

    @NotBlank(message = "{user-update-password-request.validation.password.current.required}")
    @Size(min = 6, message = "{user-update-password-request.validation.password.current.size}")
    private String currentPassword;

    @NotBlank(message = "{user-update-password-request.validation.password.new.empty}")
    @Size(min = 6, message = "{user-update-password-request.validation.password.new.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$",
            message = "{user-update-password-request.validation.password.new.invalid}"
    )
    private String newPassword;

    @NotBlank(message = "{user-update-password-request.validation.password.confirm.empty}")
    private String confirmPassword;
}