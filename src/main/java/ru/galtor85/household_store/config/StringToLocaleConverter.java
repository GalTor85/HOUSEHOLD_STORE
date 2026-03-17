package ru.galtor85.household_store.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class StringToLocaleConverter implements Converter<String, Locale> {

    /**
     * Преобразует строку в Locale - разбирает список языков и возвращает первый язык
     * Например, "ru,en-US;q=0.9" -> "ru"
     * Для Swagger UI
     */

    @Override
    public Locale convert(String source) {
        if (!StringUtils.hasText(source)) {
            return Locale.forLanguageTag("ru");
        }

        // Берем первый язык из списка
        String firstLang = source.split(",")[0].trim();

        // Убираем q-фактор
        if (firstLang.contains(";")) {
            firstLang = firstLang.split(";")[0].trim();
        }

        return Locale.forLanguageTag(firstLang);
    }
}