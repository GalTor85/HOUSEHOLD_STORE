package ru.galtor85.household_store.util.email;

import org.springframework.stereotype.Component;

@Component
public class EmailAnonymizer {

    public String anonymizeEmail(Long userId, String originalEmail) {
        return "deleted_" + userId + "_" + originalEmail;
    }

    public String anonymizeEmailWithPrefix(Long userId, String originalEmail, String prefix) {
        return prefix + "_" + userId + "_" + originalEmail;
    }

    public boolean isAnonymized(String email) {
        return email != null && email.startsWith("deleted_");
    }
}