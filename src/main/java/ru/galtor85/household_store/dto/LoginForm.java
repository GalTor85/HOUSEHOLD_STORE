package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
public class LoginForm {

    private String email;

    private String mobileNumber;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @AssertTrue(message = "Необходимо указать либо email, либо номер телефона")
    public boolean isEitherEmailOrMobilePresent() {
        return Strings.isNotBlank(email) || Strings.isNotBlank(mobileNumber);
    }

    @AssertTrue(message = "Некорректный формат email")
    public boolean isValidEmail() {
        // Если email не указан, то валидация не требуется
        if (Strings.isBlank(email)) {
            return true;
        }
        // Простая проверка формата email
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @AssertTrue(message = "Некорректный формат номера телефона")
    public boolean isValidMobileNumber() {
        // Если телефон не указан, то валидация не требуется
        if (Strings.isBlank(mobileNumber)) {
            return true;
        }
        // Проверка: только цифры, плюс, скобки, дефисы, пробелы, длина от 10 до 15 символов
        String cleaned = mobileNumber.replaceAll("[^0-9]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }
}