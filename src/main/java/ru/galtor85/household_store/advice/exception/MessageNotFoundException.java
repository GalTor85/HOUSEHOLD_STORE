package ru.galtor85.household_store.advice.exception;

import java.util.Locale;

public class MessageNotFoundException extends RuntimeException {

    private final String messageKey;
    private final Locale locale;

    public MessageNotFoundException(String messageKey, Locale locale) {
        super(String.format("Message key '%s' not found for locale '%s'", messageKey, locale));
        this.messageKey = messageKey;
        this.locale = locale;
    }

    public MessageNotFoundException(String messageKey, Locale locale, Throwable cause) {
        super(String.format("Message key '%s' not found for locale '%s'", messageKey, locale), cause);
        this.messageKey = messageKey;
        this.locale = locale;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Locale getLocale() {
        return locale;
    }
}