package ru.galtor85.household_store.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

/**
 * Request DTO for user login.
 */
@Data
@Schema(description = "Login form for user authentication", title = "Login Form")
public class LoginFormRequest {

    @Schema(description = "User email address",
            example = "user@example.com")
    private String email;

    @Schema(description = "User mobile phone number",
            example = "+79863137307")
    private String mobileNumber;

    @NotBlank(message = "{login.validation.password.empty}")
    @Pattern(
            regexp = PASSWORD_PATTERN,
            message = "{login.validation.password.invalid}"
    )
    @Schema(description = "User password",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}