package ru.galtor85.household_store.advice.exception.system;

import lombok.Getter;

import java.util.Locale;

/**
 * Exception thrown when a message key is not found in MessageSource.
 */
@Getter
public class MessageNotFoundException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Message key '%s' not found for locale '%s'";

    /**
     * Returns the missing message key.
     */
    private final String messageKey;
    /**
     * * Returns the requested locale.
     */
    private final Locale locale;

    /**
     * Constructor with message key and locale.
     *
     * @param messageKey the missing message key
     * @param locale the requested locale
     */
    public MessageNotFoundException(String messageKey, Locale locale) {
        super(String.format(MESSAGE_FORMAT, messageKey, locale));
        this.messageKey = messageKey;
        this.locale = locale;
    }

    /**
     * Constructor with message key, locale and cause.
     *
     * @param messageKey the missing message key
     * @param locale the requested locale
     * @param cause the cause
     */
    public MessageNotFoundException(String messageKey, Locale locale, Throwable cause) {
        super(String.format(MESSAGE_FORMAT, messageKey, locale), cause);
        this.messageKey = messageKey;
        this.locale = locale;
    }

}