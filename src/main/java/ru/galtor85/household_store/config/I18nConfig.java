package ru.galtor85.household_store.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

import static ru.galtor85.household_store.constants.TechnicalConstants.UTF_8_ENCODING;

/**
 * Configuration class for internationalization (i18n) and localization (l10n).
 *
 * <p>This class configures Spring's message source for loading localized messages
 * from property files and sets up locale resolution based on HTTP Accept-Language headers.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>UTF-8 encoded message bundles (supports Cyrillic, special characters)</li>
 *   <li>Reloadable message source – changes to .properties files are picked up without restart</li>
 *   <li>Configurable default locale (set via {@code app.default.locale} property)</li>
 *   <li>Separate message source for Swagger/OpenAPI documentation</li>
 *   <li>Custom converter for String → Locale conversion (for Swagger UI locale switching)</li>
 *   <li>Fallback to system locale is disabled – missing keys return the key itself</li>
 * </ul>
 *
 * <h3>Message Bundle Files Location:</h3>
 * <pre>
 * src/main/resources/
 *   ├── messages.properties          (default/base bundle English translations)
 *   ├── messages_ru.properties       (Russian translations)
 *   └── swagger/
 *       └── swagger-general.properties (Swagger UI translations)
 * </pre>
 *
 * <h3>Configuration Properties:</h3>
 * <pre>
 * app.default.locale=ru           # Default locale tag (e.g., ru, en, de)
 * </pre>
 *
 * <p><b>Note:</b> Business configuration (like cart expiry days, pagination defaults,
 * discount percentages) should NOT be stored in message bundles. Use {@code application.properties}
 * or dedicated configuration classes instead.</p>
 *
 * @author G@LTor85
 * @see MessageSource
 * @see ReloadableResourceBundleMessageSource
 * @see LocaleResolver
 * @since 1.0
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * Base name for application message bundles.
     *
     * <p>Resolves to {@code classpath:messages.properties} and
     * locale-specific variants like {@code messages_en.properties}.</p>
     */
    private static final String MESSAGE_BUNDLE_BASENAME = "classpath:messages";

    /**
     * Base name for Swagger/OpenAPI message bundles.
     *
     * <p>Resolves to {@code classpath:swagger/swagger-general.properties} and
     * locale-specific variants.</p>
     */
    private static final String SWAGGER_BUNDLE_BASENAME = "classpath:swagger/swagger-general";

    /**
     * Default locale tag injected from application.properties.
     *
     * <p>Example values: "ru", "en", "de", "fr", etc.</p>
     */
    @Value("${app.default.locale:ru}")
    private String defaultLocaleTag;

    /**
     * Internal configuration for Web MVC components.
     *
     * <p>Registers custom converters used by Spring MVC, including
     * {@link StringToLocaleConverter} for Swagger UI locale switching.</p>
     */
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        /**
         * Converter for transforming a String representation of a locale
         * (e.g., "en", "ru_RU", "en-US") into a {@link Locale} object.
         *
         * <p>This is primarily used by Swagger/OpenAPI UI to support
         * dynamic locale switching in the documentation interface.</p>
         */
        @Autowired
        private StringToLocaleConverter stringToLocaleConverter;

        /**
         * Adds custom formatters and converters to Spring's formatting registry.
         *
         * @param registry the formatter registry to register converters with
         */
        @Override
        public void addFormatters(FormatterRegistry registry) {
            registry.addConverter(stringToLocaleConverter);
        }
    }

    /**
     * Configures the locale resolver that determines which locale to use
     * for each incoming HTTP request.
     *
     * <p>The {@link AcceptHeaderLocaleResolver} extracts the locale from the
     * {@code Accept-Language} HTTP header. This is the standard approach for
     * REST APIs and web applications.</p>
     *
     * <p>If no locale information is present in the request, the default locale
     * configured via {@code app.default.locale} property is used.</p>
     *
     * @return the locale resolver bean
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag(defaultLocaleTag));
        return resolver;
    }

    /**
     * Configures the main message source for application-wide internationalization.
     *
     * <p>Uses {@link ReloadableResourceBundleMessageSource} which supports:
     * <ul>
     *   <li>UTF-8 encoding for full Unicode support (including Cyrillic)</li>
     *   <li>Hot-reloading of message bundles without application restart</li>
     *   <li>Fallback to base bundle when a translation is missing</li>
     * </ul>
     * </p>
     *
     * <p><b>Important:</b> {@code fallbackToSystemLocale=false} ensures that when a
     * message key is not found, the key itself is returned rather than falling back
     * to the system's default locale. This helps identify missing translations.</p>
     *
     * @return the main message source bean
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(MESSAGE_BUNDLE_BASENAME);
        messageSource.setDefaultEncoding(UTF_8_ENCODING);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * Configures a separate message source specifically for Swagger/OpenAPI
     * documentation internationalization.
     *
     * <p>This allows Swagger UI to display localized labels, descriptions,
     * and other documentation elements independently from the application's
     * business messages.</p>
     *
     * @return the Swagger message source bean
     */
    @Bean
    public MessageSource swaggerGeneralMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(SWAGGER_BUNDLE_BASENAME);
        messageSource.setDefaultEncoding(UTF_8_ENCODING);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}