package ru.galtor85.household_store.config;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Converter for transforming Accept-Language header to Locale.
 * Used by Swagger UI for locale switching.
 */
@Component
public class StringToLocaleConverter implements Converter<String, Locale> {

    private static final String DEFAULT_LOCALE = "ru";
    private static final String COMMA_SEPARATOR = ",";
    private static final String Q_FACTOR_SEPARATOR = ";";
    private static final int FIRST_ELEMENT = 0;

    /**
     * Converts Accept-Language header string to Locale.
     * Example: "ru,en-US;q=0.9" -> "ru"
     *
     * @param source Accept-Language header value
     * @return Locale for the first language in the list
     */
    @Override
    public Locale convert(@Nullable String source) {
        if (!StringUtils.hasText(source)) {
            return Locale.forLanguageTag(DEFAULT_LOCALE);
        }

        String firstLang = source.split(COMMA_SEPARATOR)[FIRST_ELEMENT].trim();

        if (firstLang.contains(Q_FACTOR_SEPARATOR)) {
            firstLang = firstLang.split(Q_FACTOR_SEPARATOR)[FIRST_ELEMENT].trim();
        }

        return Locale.forLanguageTag(firstLang);
    }
}