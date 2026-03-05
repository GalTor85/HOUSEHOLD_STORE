package ru.galtor85.household_store.dto;


import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditUserRequest {
    private String firstName;
    private String lastName;
    private String surname;
    private String email;
    private String mobileNumber;
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&*!])[A-Za-z\\d@#$%^&*!]{6,}$",  // Пример регулярного выражения
            message = "Пароль должен содержать минимум 6 символов, включая хотя бы одну заглавную букву, одну строчную букву, одну цифру и один специальный символ"
    )
    private String password;
    private String birthDate;
    private String address;
}
