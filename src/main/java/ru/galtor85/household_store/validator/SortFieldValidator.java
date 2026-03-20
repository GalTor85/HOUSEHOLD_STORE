package ru.galtor85.household_store.validator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class SortFieldValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id", "email", "firstName", "lastName", "mobileNumber", "createdAt"
    ));

    public String validateAndGetSortField(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "id";
        }

        String trimmed = sort.trim();
        return ALLOWED_SORT_FIELDS.contains(trimmed) ? trimmed : "id";
    }

    public boolean isValidSortField(String sort) {
        return sort != null && ALLOWED_SORT_FIELDS.contains(sort.trim());
    }
}