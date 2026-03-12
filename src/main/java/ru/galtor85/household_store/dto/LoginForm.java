package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
public class LoginForm {

    private String email;

    private String mobileNumber;

    @NotBlank(message = "{login-form.validation.password.empty}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$",
            message = "{login-form.validation.password.invalid}"
    )
    private String password;

    @AssertTrue(message = "{login-form.validation.login.identifier.required}")
    public boolean isEitherEmailOrMobilePresent() {
        return Strings.isNotBlank(email) || Strings.isNotBlank(mobileNumber);
    }

    @AssertTrue(message = "{login-form.validation.email.invalid}")
    public boolean isValidEmail() {
        if (Strings.isBlank(email)) {
            return true;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @AssertTrue(message = "{login-form.validation.mobile.invalid}")
    public boolean isValidMobileNumber() {
        if (Strings.isBlank(mobileNumber)) {
            return true;
        }
        String cleaned = mobileNumber.replaceAll("[^0-9]", "");
        return cleaned.length() >= 5 && cleaned.length() <= 15;
    }

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