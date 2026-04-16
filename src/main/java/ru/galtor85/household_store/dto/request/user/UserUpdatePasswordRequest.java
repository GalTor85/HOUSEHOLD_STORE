package ru.galtor85.household_store.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.galtor85.household_store.constants.TechnicalConstants.PASSWORD_PATTERN;

/**
 * Request DTO for updating user password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating user password")
public class UserUpdatePasswordRequest {

    @NotBlank(message = "{user-update-password-request.validation.password.current.required}")
    @Schema(description = "Current password", example = "OldPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "{user-update-password-request.validation.password.new.empty}")
    @Pattern(regexp = PASSWORD_PATTERN, message = "{user-update-password-request.validation.password.new.invalid}")
    @Schema(description = "New password (min 6 chars, at least one digit, one lowercase, one uppercase, one special character)",
            example = "NewPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @NotBlank(message = "{user-update-password-request.validation.password.confirm.empty}")
    @Schema(description = "Confirm new password", example = "NewPassword123!", requiredMode = Schema.RequiredMode.REQUIRED  )
    private String confirmPassword;
}