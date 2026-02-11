package ru.galtor85.household_store.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MobileNumber {
    @NotNull(message = "Номер телефона не может быть пустым")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",  // Пример регулярного выражения
            message = "Некорректный формат номера телефона"
    )
    private String phoneNumber;
}
