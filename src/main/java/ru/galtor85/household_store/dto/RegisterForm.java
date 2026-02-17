package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterForm {

    // @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;

    // @NotBlank(message = "Номер телефона обязателен")

    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",  // Пример регулярного выражения
            message = "Некорректный формат номера телефона"
    )
    private String mobileNumber;



    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    private String lastName;



    private String surname;

    private LocalDate birthDate;



}