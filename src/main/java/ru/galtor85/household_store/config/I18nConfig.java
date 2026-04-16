package ru.galtor85.household_store.config;

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
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    private static final String MESSAGE_BUNDLE_BASENAME = "classpath:messages";
    private static final String SWAGGER_BUNDLE_BASENAME = "classpath:swagger/swagger-general";

    @Value("${app.default.locale:ru}")
    private String defaultLocaleTag;

    private final StringToLocaleConverter stringToLocaleConverter;

    /**
     * Constructor with dependency injection.
     *
     * @param stringToLocaleConverter converter for Accept-Language header
     */
    public I18nConfig(StringToLocaleConverter stringToLocaleConverter) {
        this.stringToLocaleConverter = stringToLocaleConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToLocaleConverter);
    }

    /**
     * Configures locale resolver based on Accept-Language header.
     *
     * @return locale resolver bean
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag(defaultLocaleTag));
        return resolver;
    }

    /**
     * Configures main message source for application i18n.
     *
     * @return message source bean
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
     * Configures separate message source for Swagger/OpenAPI documentation.
     *
     * @return Swagger message source bean
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