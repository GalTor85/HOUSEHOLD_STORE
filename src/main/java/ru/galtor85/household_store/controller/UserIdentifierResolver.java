package ru.galtor85.household_store.controller;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.galtor85.household_store.dto.LoginForm;

@Component
public class UserIdentifierResolver {
    public static String resolve(LoginForm form) {
        if (StringUtils.hasText(form.getEmail())) return form.getEmail();
        if (StringUtils.hasText(form.getMobileNumber())) return form.getMobileNumber();
        throw new IllegalArgumentException("No identifier provided");
    }
}