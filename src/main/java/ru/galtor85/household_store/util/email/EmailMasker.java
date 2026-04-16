package ru.galtor85.household_store.util.email;

import org.springframework.stereotype.Component;

import static ru.galtor85.household_store.constants.TechnicalConstants.EMAIL_PATTERN;
import static ru.galtor85.household_store.constants.TechnicalConstants.KEEP_ONLY_DIGITS;

/**
 * Utility component for masking sensitive user identifiers in logs and displays.
 *
 * <p>Provides methods to safely mask email addresses and phone numbers,
 * ensuring that sensitive personal data is not exposed in application logs
 * or non-secure UI contexts.</p>
 *
 * <p>Masking rules:
 * <ul>
 *   <li><b>Email:</b> Shows first and last character of local part, masks middle</li>
 *   <li><b>Phone:</b> Shows only last 4 digits, masks the rest</li>
 *   <li><b>Unknown format:</b> Returns generic masked placeholder</li>
 * </ul>
 *
 * @author G@LTor85
 
 */
@Component
public class EmailMasker {

    private static final int MASK_VISIBLE_PHONE_DIGITS = 4;
    private static final int MIN_LOCAL_PART_FOR_PARTIAL_MASK = 3;

    private static final String MASKED_EMAIL_MIDDLE = "***";
    private static final String MASKED_PHONE_PREFIX = "***-***-";
    private static final String MASKED_SHORT = "***";
    private static final String MASKED_NULL = "null";

    private static final String EMAIL_AT_SIGN = "@";

    /**
     * Masks an email address for secure logging.
     *
     * <p>Examples:
     * <ul>
     *   <li>"john.doe@example.com" → "j***e@example.com"</li>
     *   <li>"ab@example.com" → "**@example.com"</li>
     *   <li>null → "null"</li>
     * </ul>
     *
     * @param email the email address to mask
     * @return masked email address
     */
    public String maskEmail(String email) {
        if (email == null) {
            return MASKED_NULL;
        }

        if (!isEmail(email)) {
            return maskUnknown(email);
        }

        int atIndex = email.indexOf(EMAIL_AT_SIGN);
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() < MIN_LOCAL_PART_FOR_PARTIAL_MASK) {
            return "*".repeat(localPart.length()) + domain;
        }

        return localPart.charAt(0) + MASKED_EMAIL_MIDDLE + localPart.charAt(localPart.length() - 1) + domain;
    }

    /**
     * Masks a phone number for secure logging.
     *
     * <p>Removes all non-digit characters and shows only the last 4 digits.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>"+7 (999) 123-45-67" → "***-***-4567"</li>
     *   <li>"123" → "***"</li>
     *   <li>null → "null"</li>
     * </ul>
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return MASKED_NULL;
        }

        String digits = phoneNumber.replaceAll(KEEP_ONLY_DIGITS, "");

        if (digits.length() >= MASK_VISIBLE_PHONE_DIGITS) {
            return MASKED_PHONE_PREFIX + digits.substring(digits.length() - MASK_VISIBLE_PHONE_DIGITS);
        }

        return MASKED_SHORT;
    }

    /**
     * Automatically detects identifier type and applies appropriate masking.
     *
     * <p>Detection rules:
     * <ul>
     *   <li>Contains '@' and matches email pattern → email masking</li>
     *   <li>Otherwise → phone number masking</li>
     * </ul>
     *
     * @param identifier the identifier to mask (email or phone number)
     * @return masked identifier safe for logging
     */
    public String maskIdentifier(String identifier) {
        if (identifier == null) {
            return MASKED_NULL;
        }

        if (isEmail(identifier)) {
            return maskEmail(identifier);
        }

        return maskPhoneNumber(identifier);
    }

    /**
     * Checks if the provided string is a valid email address.
     *
     * @param value the string to check
     * @return true if the string matches email pattern
     */
    public boolean isEmail(String value) {
        return value != null && value.contains(EMAIL_AT_SIGN) && value.matches(EMAIL_PATTERN);
    }

    /**
     * Fallback masking for unknown identifier formats.
     *
     * @param value the value to mask
     * @return masked value based on length
     */
    private String maskUnknown(String value) {
        if (value == null) {
            return MASKED_NULL;
        }

        if (value.length() <= MIN_LOCAL_PART_FOR_PARTIAL_MASK) {
            return MASKED_SHORT;
        }

        return value.charAt(0) + MASKED_EMAIL_MIDDLE + value.charAt(value.length() - 1);
    }
}