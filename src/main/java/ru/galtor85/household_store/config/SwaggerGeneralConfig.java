package ru.galtor85.household_store.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * Configuration for Swagger/OpenAPI documentation with i18n support.
 */
@SecurityScheme(name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
@Configuration
public class SwaggerGeneralConfig {

    private final MessageSource swaggerGeneralMessageSource;

    public SwaggerGeneralConfig(@Qualifier("swaggerGeneralMessageSource") MessageSource swaggerGeneralMessageSource) {
        this.swaggerGeneralMessageSource = swaggerGeneralMessageSource;
    }

    /**
     * Creates OpenAPI configuration with localized texts.
     *
     * @return OpenAPI instance
     */
    @Bean(name = "baseOpenAPI")
    public OpenAPI customOpenAPI() {
        Locale locale = LocaleContextHolder.getLocale();
        return new OpenAPI()
                .info(new Info()
                        .title(getMessage("swagger-general-config.app.name", locale))
                        .description(getMessage("swagger-general-config.app.description", locale))
                        .version(getMessage("swagger-general-config.app.version", locale))
                        .contact(new Contact()
                                .name(getMessage("swagger-general-config.contact.name", locale))
                                .email(getMessage("swagger-general-config.contact.email", locale)))
                        .license(new License()
                                .name(getMessage("swagger-general-config.license.name", locale))
                                ));
    }

    private String getMessage(String code, Locale locale) {
        return swaggerGeneralMessageSource.getMessage(code, null, locale);
    }
}