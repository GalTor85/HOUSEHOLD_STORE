package ru.galtor85.household_store.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.galtor85.household_store.advice.exception.auth.IdentifierNotProvidedException;
import ru.galtor85.household_store.dto.request.auth.LoginFormRequest;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.util.email.EmailMasker;


/**
 * Resolver for extracting user identifier from login form.
 *
 * <p>Determines which identifier (email or mobile number) the user provided
 * during login. The resolver prioritizes email over mobile number if both
 * are somehow present (though validation should prevent this).</p>
 *
 * <p>Resolution priority:
 * <ol>
 *   <li>Email (if provided and not blank)</li>
 *   <li>Mobile number (if provided and not blank)</li>
 *   <li>Throws {@link IdentifierNotProvidedException} if neither is provided</li>
 * </ol>
 *
 * @author G@LTor85
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdentifierResolver {

    private final LogMessageService logMsg;
    private final EmailMasker emailMasker;

    /**
     * Resolves the user identifier from the login form.
     *
     * <p>Returns the email if present, otherwise the mobile number.</p>
     *
     * @param form the login form containing email and/or mobile number
     * @return the resolved identifier (email or mobile number)
     * @throws IdentifierNotProvidedException if neither email nor mobile number is provided
     */
    public String resolve(LoginFormRequest form) {

        if (StringUtils.hasText(form.getEmail())) {
            log.debug(logMsg.get(
                    "user-identifier-resolver.log.identifier.using.email",
                    emailMasker.maskEmail(form.getEmail())
            ));
            return form.getEmail();
        }

        if (StringUtils.hasText(form.getMobileNumber())) {
            log.debug(logMsg.get(
                    "user-identifier-resolver.log.identifier.using.mobile",
                    emailMasker.maskPhoneNumber(form.getMobileNumber())
            ));
            return form.getMobileNumber();
        }

        log.warn(logMsg.get("user-identifier-resolver.log.identifier.not.provided"));
        throw new IdentifierNotProvidedException();
    }
}