package ru.galtor85.household_store.service.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Returns a localized message with smart fallback.
     */
    public String get(String code, Object... args) {
        return getInternal(code, args);
    }

    /**
     * Returns a localized message without arguments.
     */
    public String get(String code) {
        return getInternal(code);
    }

    /**
     * Returns a localized message with explicit default fallback.
     */
    public String getWithDefault(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key '{}' not found for locale '{}'", code, locale);
            return defaultMessage != null ? defaultMessage : code;
        } catch (Exception e) {
            log.error("Error resolving with default message key '{}': {}", code, e.getMessage());
            return defaultMessage != null ? defaultMessage : code;
        }
    }

    private String getInternal(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            String message = messageSource.getMessage(code, args, locale);
            if (!message.equals(code)) {
                return message;
            }
        } catch (NoSuchMessageException e) {
            log.debug("Message key '{}' not found for locale '{}'", code, locale);
        } catch (Exception e) {
            log.error("Error resolving message key '{}': {}", code, e.getMessage());
        }

        return buildReadableFallback(code, args);
    }

    private String buildReadableFallback(String code, Object... args) {
        String readable = extractReadablePart(code);
        if (!readable.isEmpty()) {
            readable = Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
        }
        if (args != null && args.length > 0) {
            return readable + ": " + formatArgs(args);
        }
        return readable;
    }

    private String extractReadablePart(String code) {
        if (code == null || code.isEmpty()) {
            return "Unknown";
        }
        String[] parts = code.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (isMeaningfulPart(part)) {
                if (isActionWord(part) && i > 0 && isMeaningfulPart(parts[i - 1])) {
                    return parts[i - 1].replace('_', ' ') + " " + part.replace('_', ' ');
                }
                return part.replace('_', ' ');
            }
        }
        return parts[parts.length - 1].replace('_', ' ');
    }

    private boolean isActionWord(String part) {
        return part.equals("start") || part.equals("success") || part.equals("complete") ||
                part.equals("error") || part.equals("failed") || part.equals("created") ||
                part.equals("updated") || part.equals("deleted") || part.equals("processed") ||
                part.equals("fetched") || part.equals("found") || part.equals("saved") ||
                part.equals("invalid") || part.equals("required") || part.equals("empty");
    }

    private boolean isMeaningfulPart(String part) {
        return !part.equals("user") && !part.equals("manager") && !part.equals("admin") &&
                !part.equals("service") && !part.equals("controller") && !part.equals("processor") &&
                !part.equals("validator") && !part.equals("handler") && !part.equals("repository") &&
                !part.equals("log") && !part.equals("error") && !part.equals("validation");
    }

    private String formatArgs(Object... args) {
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}