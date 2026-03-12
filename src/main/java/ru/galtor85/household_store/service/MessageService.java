package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import ru.galtor85.household_store.advice.exception.MessageNotFoundException;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Получение сообщения по ключу с параметрами.
     * Если ключ не найден, возвращает сам ключ и логирует ошибку.
     *
     * @param code ключ сообщения
     * @param args параметры для подстановки
     * @return сообщение или ключ, если сообщение не найдено
     */
    public String get(String code, Object... args) {
        return getWithFallback(code, null, false, args);
    }

    /**
     * Получение сообщения по ключу без параметров.
     * Если ключ не найден, возвращает сам ключ и логирует ошибку.
     *
     * @param code ключ сообщения
     * @return сообщение или ключ, если сообщение не найдено
     */
    public String get(String code) {
        return getWithFallback(code, null, false);
    }

    /**
     * Получение сообщения по ключу с возможностью указать значение по умолчанию.
     * Если ключ не найден, возвращает defaultMessage и логирует ошибку.
     *
     * @param code ключ сообщения
     * @param defaultMessage значение по умолчанию
     * @param args параметры для подстановки
     * @return сообщение или defaultMessage, если сообщение не найдено
     */
    public String getWithDefault(String code, String defaultMessage, Object... args) {
        return getWithFallback(code, defaultMessage, false, args);
    }

    /**
     * Получение сообщения по ключу с выбрасыванием исключения при отсутствии.
     * Использовать только для критических сообщений, без которых приложение не может работать.
     *
     * @param code ключ сообщения
     * @param args параметры для подстановки
     * @return сообщение
     * @throws MessageNotFoundException если ключ не найден
     */
    public String getRequired(String code, Object... args) {
        return getWithFallback(code, null, true, args);
    }

    /**
     * Внутренний метод с fallback-логикой
     */
    private String getWithFallback(String code, String defaultMessage, boolean throwException, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String message = messageSource.getMessage(code, args, locale);

            if (message != null && message.equals(code)) {
                log.warn("Message key '{}' resolved to itself - possible missing translation for locale: {}",
                        code, locale);

                if (throwException) {
                    throw new MessageNotFoundException(code, locale);
                }

                if (defaultMessage != null) {
                    return defaultMessage;
                }

                return code;
            }

            return message;

        } catch (Exception e) {
            log.error("Failed to resolve message key '{}' for locale {}: {}",
                    code, LocaleContextHolder.getLocale(), e.getMessage());

            if (throwException) {
                throw new MessageNotFoundException(code, LocaleContextHolder.getLocale(), e);
            }

            if (defaultMessage != null) {
                return defaultMessage;
            }

            return code;
        }
    }

    /**
     * Проверка существования ключа в MessageSource
     *
     * @param code ключ сообщения
     * @return true если ключ существует, иначе false
     */
    public boolean exists(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String message = messageSource.getMessage(code, null, locale);
            return message != null && !message.equals(code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получение сообщения с форматированием и логированием ключа в случае ошибки
     */
    public String getWithKeyLogging(String code, Object... args) {
        String result = get(code, args);

        if (result.equals(code)) {
            log.debug("Message key '{}' not found, using key as fallback", code);
        }

        return result;
    }
}