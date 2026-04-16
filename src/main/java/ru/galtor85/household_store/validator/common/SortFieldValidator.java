package ru.galtor85.household_store.validator.common;

import org.springframework.stereotype.Component;
import ru.galtor85.household_store.constants.PaginationConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator for sort fields in pagination requests.
 *
 * <p>This validator ensures that only allowed sort fields are used
 * to prevent SQL injection and invalid sort operations.</p>
 *
 * @author G@LTor85
 
 */
@Component
public class SortFieldValidator {

    /**
     * Allowed sort fields for user queries.
     * These are database column names, not configurable via properties.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id",
            "email",
            "firstName",
            "lastName",
            "mobileNumber",
            "createdAt"
    ));

    /**
     * Validates and returns the sort field.
     * Returns default sort field if input is invalid or null.
     *
     * @param sort the sort field to validate
     * @return valid sort field or default value
     */
    public String validateAndGetSortField(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return PaginationConstants.DEFAULT_SORT_FIELD;
        }

        String trimmed = sort.trim();
        return ALLOWED_SORT_FIELDS.contains(trimmed) ? trimmed : PaginationConstants.DEFAULT_SORT_FIELD;
    }
}