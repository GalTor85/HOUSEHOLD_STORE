package ru.galtor85.household_store.controller.resolve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.galtor85.household_store.advice.exception.IdentifierNotProvidedException;
import ru.galtor85.household_store.dto.LoginForm;
import ru.galtor85.household_store.service.MessageService;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdentifierResolver {

    private final MessageService messageService;

    public String resolve(LoginForm form, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        if (StringUtils.hasText(form.getEmail())) {
            log.debug(messageService.get(
                    "user-identifier-resolver.log.identifier.using.email",
                    form.getEmail()
            ));
            return form.getEmail();
        }

        if (StringUtils.hasText(form.getMobileNumber())) {
            log.debug(messageService.get(
                    "user-identifier-resolver.log.identifier.using.mobile",
                    form.getMobileNumber()
            ));
            return form.getMobileNumber();
        }

        log.warn(messageService.get("user-identifier-resolver.log.identifier.not.provided"));
        throw new IdentifierNotProvidedException();
    }

    public String resolve(LoginForm form) {
        return resolve(form, Locale.getDefault());
    }
}