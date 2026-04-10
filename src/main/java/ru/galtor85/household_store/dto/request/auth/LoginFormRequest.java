package ru.galtor85.household_store.dto.request.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

import static ru.galtor85.household_store.constants.TechnicalConstants.*;

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

    /**
     * Validates that either email or mobile number is provided (but not both)
     * This is a cross-field validation using XOR logic
     *
     * @return true if exactly one of email or mobileNumber is provided
     */
    @AssertTrue(message = "{login.validation.identifier.required}")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isEitherEmailOrMobilePresent() {
        return Strings.isNotBlank(email) ^ Strings.isNotBlank(mobileNumber);
    }

    /**
     * Validates email format if email is provided
     *
     * @return true if email is blank or matches valid email pattern
     */
    @AssertTrue(message = "{login.validation.email.invalid}")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidEmail() {
        if (Strings.isBlank(email)) {
            return true;
        }
        return email.matches(EMAIL_PATTERN);
    }

    /**
     * Validates phone number format if mobile number is provided
     * Phone number must contain 5 to 15 digits (plus signs and dashes are ignored)
     *
     * @return true if mobile number is blank or contains 5-15 digits
     */
    @AssertTrue(message = "{login.validation.mobile.invalid}")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidMobileNumber() {
        if (Strings.isBlank(mobileNumber)) {
            return true;
        }
        String cleaned = mobileNumber.replaceAll(KEEP_ONLY_DIGITS, "");
        return cleaned.length() >= MIN_PHONE_LENGTH && cleaned.length() <= MAX_PHONE_LENGTH;
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