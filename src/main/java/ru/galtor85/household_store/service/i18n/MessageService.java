package ru.galtor85.household_store.service.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.advice.exception.system.MessageNotFoundException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Service for retrieving localized messages for API responses and UI.
 * Falls back to a readable message generated from the key if translation is missing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Returns a localized message for the given key.
     * If key not found, logs warning and returns readable fallback generated from the key.
     *
     * @param code message key
     * @param args formatting arguments
     * @return localized message or generated fallback
     */
    public String get(String code, Object... args) {
        return getInternal(code, null, false, true, args);
    }

    /**
     * Returns a localized message without arguments.
     */
    public String get(String code) {
        return getInternal(code, null, false, true);
    }

    /**
     * Returns a localized message with explicit default fallback.
     *
     * @param code           message key
     * @param defaultMessage fallback message if key not found
     * @param args           formatting arguments
     * @return localized message or defaultMessage
     */
    public String getWithDefault(String code, String defaultMessage, Object... args) {
        return getInternal(code, defaultMessage, false, false, args);
    }

    /**
     * Returns a localized message, throws exception if key not found.
     * Use only for critical messages required for application to function.
     *
     * @param code message key
     * @param args formatting arguments
     * @return localized message
     * @throws MessageNotFoundException if key not found
     */
    public String getRequired(String code, Object... args) {
        return getInternal(code, null, true, false, args);
    }

    /**
     * Internal method with fallback logic.
     */
    private String getInternal(String code, String defaultMessage, boolean throwException,
                               boolean useSmartFallback, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        // Try to resolve exact key
        try {
            String message = messageSource.getMessage(code, args, locale);
            if (!message.equals(code)) {
                return message;
            }
        } catch (NoSuchMessageException e) {
            // Key not found - will handle below
        } catch (Exception e) {
            log.error("Error resolving message key '{}': {}", code, e.getMessage());
        }

        // Key not found - log it
        log.warn("Message key '{}' not found for locale '{}'", code, locale);

        // Handle according to parameters
        if (throwException) {
            throw new MessageNotFoundException(code, locale);
        }

        if (defaultMessage != null) {
            return defaultMessage;
        }

        // Generate smart fallback from the key itself
        if (useSmartFallback) {
            return buildReadableFallback(code, args);
        }

        return code;
    }

    /**
     * Builds a readable fallback message from the message key.
     * Example: "order.status.UNKNOWN" → "Order status: UNKNOWN"
     * Example: "validation.email.invalid" → "Email invalid"
     */
    private String buildReadableFallback(String code, Object... args) {
        String readable = extractReadablePart(code);

        // Capitalize first letter
        if (!readable.isEmpty()) {
            readable = Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
        }

        // Add arguments if present
        if (args != null && args.length > 0) {
            return readable + ": " + formatArgs(args);
        }
        return readable;
    }

    /**
     * Extracts the most meaningful part from the message key.
     */
    private String extractReadablePart(String code) {
        if (code == null || code.isEmpty()) {
            return "Unknown";
        }

        String[] parts = code.split("\\.");

        // Find the last meaningful part
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (isMeaningfulPart(part)) {
                // If it's an action word, include the context
                if (isActionWord(part) && i > 0 && isMeaningfulPart(parts[i - 1])) {
                    return parts[i - 1].replace('_', ' ') + " " + part.replace('_', ' ');
                }
                return part.replace('_', ' ');
            }
        }

        // Fallback to last part
        return parts[parts.length - 1].replace('_', ' ');
    }

    /**
     * Checks if the part is an action word (verb).
     */
    private boolean isActionWord(String part) {
        return part.equals("start") || part.equals("success") || part.equals("complete") ||
                part.equals("error") || part.equals("failed") || part.equals("created") ||
                part.equals("updated") || part.equals("deleted") || part.equals("processed") ||
                part.equals("fetched") || part.equals("found") || part.equals("saved") ||
                part.equals("invalid") || part.equals("required") || part.equals("empty");
    }

    /**
     * Checks if the part is meaningful (not a technical prefix).
     */
    private boolean isMeaningfulPart(String part) {
        return !part.equals("user") && !part.equals("manager") && !part.equals("admin") &&
                !part.equals("service") && !part.equals("controller") && !part.equals("processor") &&
                !part.equals("validator") && !part.equals("handler") && !part.equals("repository") &&
                !part.equals("log") && !part.equals("error") && !part.equals("validation");
    }

    /**
     * Formats arguments for display in the fallback message.
     */
    private String formatArgs(Object... args) {
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * Checks if a message key exists in the MessageSource.
     *
     * @param code message key
     * @return true if key exists, false otherwise
     */
    public boolean exists(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String message = messageSource.getMessage(code, null, locale);
            return !message.equals(code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns message with key logging for debugging.
     */
    public String getWithKeyLogging(String code, Object... args) {
        String result = get(code, args);
        if (result.equals(code)) {
            log.debug("Message key '{}' not found, using key as fallback", code);
        }
        return result;
    }

    /**
     * Returns message with line separator appended.
     */
    public String getLn(String code, Object... args) {
        return get(code, args) + System.lineSeparator();
    }

    /**
     * Returns message with line separator appended (no arguments).
     */
    public String getLn(String code) {
        return get(code) + System.lineSeparator();
    }
}