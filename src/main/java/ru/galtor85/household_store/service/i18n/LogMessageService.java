package ru.galtor85.household_store.service.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;

/**
 * Service for retrieving log messages in a fixed configured locale.
 * Falls back to English if the configured locale is invalid.
 * Generates smart readable fallback from key if translation is missing.
 *
 */
@Slf4j
@Service
public class LogMessageService {

    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;

    private final MessageSource messageSource;
    private final Locale logLocale;

    public LogMessageService(
            MessageSource messageSource,
            @Value("${app.logging.locale:en}") String localeTag) {
        this.messageSource = messageSource;
        this.logLocale = resolveLocale(localeTag);
        log.info("LogMessageService initialized: locale={} (from '{}')", logLocale, localeTag);
    }

    /**
     * Resolves locale from configuration tag with fallback to English.
     */
    private Locale resolveLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return FALLBACK_LOCALE;
        }

        try {
            Locale locale = Locale.forLanguageTag(localeTag);
            if (locale.getLanguage().isEmpty()) {
                return FALLBACK_LOCALE;
            }
            return locale;
        } catch (Exception e) {
            return FALLBACK_LOCALE;
        }
    }

    /**
     * Returns localized message for logging.
     * If key not found, generates smart readable fallback.
     *
     * @param code message key
     * @param args formatting arguments
     * @return localized message or generated fallback
     */
    public String get(String code, Object... args) {
        try {
            String message = messageSource.getMessage(code, args, logLocale);
            if (!message.equals(code)) {
                return message;
            }
        } catch (NoSuchMessageException e) {
            // Will use smart fallback
        } catch (Exception e) {
            log.error("Error resolving log message key '{}': {}", code, e.getMessage());
        }

        // Smart fallback
        return buildSmartFallback(code, args);
    }

    /**
     * Builds smart readable fallback from message key.
     */
    private String buildSmartFallback(String code, Object... args) {
        if (code == null || code.isEmpty()) {
            return "Unknown";
        }

        String readable = extractSmartPart(code);

        if (args != null && args.length > 0) {
            return readable + ": " + formatArgs(args);
        }
        return readable;
    }

    /**
     * Extracts smart readable part from dotted key.
     */
    private String extractSmartPart(String code) {
        String[] parts = code.split("\\.");

        String subject = null;
        String action = null;
        String context = null;

        for (String part : parts) {
            if (isTechnicalPrefix(part)) {
                continue;
            }

            if (isActionWord(part)) {
                action = part;
            } else if (isSubjectWord(part)) {
                subject = part;
            } else if (context == null) {
                context = part;
            }
        }

        return buildNaturalPhrase(subject, action, context);
    }

    /**
     * Builds natural English phrase from extracted parts.
     */
    private String buildNaturalPhrase(String subject, String action, String context) {
        StringBuilder phrase = new StringBuilder();

        if (subject != null) {
            phrase.append(capitalize(subject.replace('_', ' ')));
        } else if (context != null) {
            phrase.append(capitalize(context.replace('_', ' ')));
        }

        if (action != null) {
            if (!phrase.isEmpty()) {
                phrase.append(" ");
            }
            phrase.append(convertToPastTense(action));
        } else {
            if (!phrase.isEmpty()) {
                phrase.append(" processed");
            } else {
                phrase.append("Operation");
            }
        }

        return phrase.toString();
    }

    /**
     * Converts action word to past tense.
     */
    private String convertToPastTense(String action) {
        return switch (action) {
            case "start" -> "started";
            case "complete" -> "completed";
            case "create" -> "created";
            case "update" -> "updated";
            case "delete" -> "deleted";
            case "process" -> "processed";
            case "fetch" -> "fetched";
            case "find" -> "found";
            case "save" -> "saved";
            case "success" -> "succeeded";
            case "fail", "failed", "error" -> "failed";
            case "cancel" -> "cancelled";
            case "assign" -> "assigned";
            case "clear" -> "cleared";
            case "upload" -> "uploaded";
            case "transfer" -> "transferred";
            case "receive" -> "received";
            case "pay" -> "paid";
            case "refund" -> "refunded";
            default -> action + "ed";
        };
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private boolean isActionWord(String part) {
        return part.equals("start") || part.equals("success") || part.equals("complete") ||
                part.equals("error") || part.equals("failed") || part.equals("fail") ||
                part.equals("create") || part.equals("created") || part.equals("update") ||
                part.equals("updated") || part.equals("delete") || part.equals("deleted") ||
                part.equals("process") || part.equals("processed") || part.equals("fetch") ||
                part.equals("fetched") || part.equals("find") || part.equals("found") ||
                part.equals("save") || part.equals("saved") || part.equals("cancel") ||
                part.equals("cancelled") || part.equals("assign") || part.equals("assigned") ||
                part.equals("clear") || part.equals("cleared") || part.equals("upload") ||
                part.equals("uploaded") || part.equals("transfer") || part.equals("transferred") ||
                part.equals("receive") || part.equals("received") || part.equals("pay") ||
                part.equals("paid") || part.equals("refund") || part.equals("refunded");
    }

    private boolean isSubjectWord(String part) {
        return part.equals("payment") || part.equals("order") || part.equals("product") ||
                part.equals("user") || part.equals("customer") || part.equals("supplier") ||
                part.equals("invoice") || part.equals("cart") || part.equals("stock") ||
                part.equals("warehouse") || part.equals("cell") || part.equals("media") ||
                part.equals("file") || part.equals("price") || part.equals("transaction") ||
                part.equals("cash") || part.equals("register") || part.equals("bank") ||
                part.equals("account") || part.equals("currency") || part.equals("category") ||
                part.equals("brand") || part.equals("shipment") || part.equals("delivery") ||
                part.equals("refund") || part.equals("rollback") || part.equals("inventory");
    }

    private boolean isTechnicalPrefix(String part) {
        return part.equals("service") || part.equals("controller") || part.equals("processor") ||
                part.equals("validator") || part.equals("handler") || part.equals("repository") ||
                part.equals("log") || part.equals("validation") || part.equals("manager") ||
                part.equals("admin");
    }

    private String formatArgs(Object... args) {
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}