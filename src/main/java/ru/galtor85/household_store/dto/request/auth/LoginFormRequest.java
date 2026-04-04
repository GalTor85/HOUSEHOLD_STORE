package ru.galtor85.household_store.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
@Schema(description = "Login form for user authentication", title = "Login Form")
public class LoginFormRequest {

    @Schema(description = "User email address",
            example = "user@example.com")
    private String email;

    @Schema(description = "User mobile phone number",
            example = "+79863137307")
    private String mobileNumber;

    @NotBlank(message = "Password cannot be empty")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$",
            message = "Password must contain at least one digit, one uppercase and one lowercase letter, and one special character (@#$%^&*!)"
    )
    @Schema(description = "User password",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * Validates that either email or mobile number is provided (but not both)
     * This is a cross-field validation using XOR logic
     *
     * @return true if exactly one of email or mobileNumber is provided
     */
    @AssertTrue(message = "Email or phone number is required")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isEitherEmailOrMobilePresent() {
        return Strings.isNotBlank(email) ^ Strings.isNotBlank(mobileNumber);  // XOR - exactly one must be true
    }

    /**
     * Validates email format if email is provided
     *
     * @return true if email is blank or matches valid email pattern
     */
    @AssertTrue(message = "Invalid email format")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidEmail() {
        if (Strings.isBlank(email)) {
            return true;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validates phone number format if mobile number is provided
     * Phone number must contain 5 to 15 digits (plus signs and dashes are ignored)
     *
     * @return true if mobile number is blank or contains 5-15 digits
     */
    @AssertTrue(message = "Invalid phone number format (must contain 5 to 15 digits)")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidMobileNumber() {
        if (Strings.isBlank(mobileNumber)) {
            return true;
        }
        String cleaned = mobileNumber.replaceAll("[^0-9]", "");
        return cleaned.length() >= 5 && cleaned.length() <= 15;
    }

    /**
     * Gets the identifier (email or mobile number) for authentication
     *
     * @return email if provided, otherwise mobile number, null if both are blank
     */
    @JsonIgnore
    @Schema(hidden = true)
    public String getIdentifier() {
        if (Strings.isNotBlank(email)) {
            return email;
        }
        if (Strings.isNotBlank(mobileNumber)) {
            return mobileNumber;
        }
        return null;
    }
}