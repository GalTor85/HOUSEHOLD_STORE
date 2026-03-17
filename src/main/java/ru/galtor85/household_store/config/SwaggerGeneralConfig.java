package ru.galtor85.household_store.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

@SecurityScheme(name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
@Configuration
@RequiredArgsConstructor
public class SwaggerGeneralConfig {

    @Qualifier("swaggerGeneralMessageSource")
    private final MessageSource swaggerGeneralMessageSource;

    @Bean(name = "baseOpenAPI")
    public OpenAPI customOpenAPI() {
        Locale locale = LocaleContextHolder.getLocale();
        return new OpenAPI()
                .info(new Info()
                        .title(swaggerGeneralMessageSource.getMessage("swagger-general-config.app.name", null, locale))
                        .description(swaggerGeneralMessageSource.getMessage("swagger-general-config.app.description", null, locale))
                        .version(swaggerGeneralMessageSource.getMessage("swagger-general-config.app.version", null, locale))
                        .contact(new Contact()
                                .name(swaggerGeneralMessageSource.getMessage("swagger-general-config.contact.name", null, locale))
                                .email(swaggerGeneralMessageSource.getMessage("swagger-general-config.contact.email", null, locale)))
                        .license(new License()
                                .name(swaggerGeneralMessageSource.getMessage("swagger-general-config.license.name", null, locale))
                                .url(swaggerGeneralMessageSource.getMessage("swagger-general-config.license.url", null, locale))));
    }
}