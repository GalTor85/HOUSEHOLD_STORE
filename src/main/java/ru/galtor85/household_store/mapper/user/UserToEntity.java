package ru.galtor85.household_store.mapper.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.request.auth.UserCreateRequest;
import ru.galtor85.household_store.dto.request.user.UserEditRequest;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.validator.auth.UserValidator;

import java.time.LocalDate;

/**
 * Mapper for creating and updating User entities from requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserToEntity {

    private final MessageService messageService;
    private final UserValidator userValidator;
    private final LogMessageService logMsg;

    /**
     * Builds a User entity from the create request.
     *
     * @param request the user creation request
     * @param creator identifier of the creator (e.g., "self-registration" or admin email)
     * @return User entity
     */
    public User build(UserCreateRequest request, String creator) {
        if (request == null) {
            log.warn(logMsg.get("user-to-entity.log.mapper.request.null"));
            return null;
        }

        userValidator.validateFirstName(request.getFirstName());
        userValidator.validateLastName(request.getLastName());
        userValidator.validateSurnameLength(request.getSurname());
        userValidator.validateEmailLength(request.getEmail());
        userValidator.validatePhoneNumberLength(request.getMobileNumber());
        userValidator.validateAddressLength(request.getAddress());

        log.debug(logMsg.get("user-to-entity.log.mapper.converting.user", request.getEmail()));

        return User.builder()
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .surname(request.getSurname())
                .birthDate(parseBirthDate(request.getBirthDate()))
                .address(request.getAddress())
                .creator(creator)
                .build();
    }

    /**
     * Parses birth date string to LocalDate.
     *
     * @param birthDate birth date as string (YYYY-MM-DD)
     * @return LocalDate or null if empty
     * @throws IllegalArgumentException if format is invalid
     */
    private LocalDate parseBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthDate);
        } catch (Exception e) {
            String errorMessage = messageService.get(
                    "user-to-entity.error.mapper.birthdate.invalid",
                    birthDate
            );
            log.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Updates an existing User entity from edit request.
     *
     * @param user    the existing user entity
     * @param request the edit request with updated values
     */
    public void updateUserFromRequest(User user, UserEditRequest request) {
        if (user == null || request == null) {
            return;
        }

        log.debug(logMsg.get("user-to-entity.log.mapper.updating.user", user.getId()));

        if (request.getEmail() != null) {
            userValidator.validateEmailLength(request.getEmail());
            user.setEmail(request.getEmail());
        }
        if (request.getMobileNumber() != null) {
            userValidator.validatePhoneNumberLength(request.getMobileNumber());
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getFirstName() != null) {
            userValidator.validateFirstName(request.getFirstName());
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            userValidator.validateLastName(request.getLastName());
            user.setLastName(request.getLastName());
        }
        if (request.getSurname() != null) {
            userValidator.validateSurnameLength(request.getSurname());
            user.setSurname(request.getSurname());
        }
        if (request.getAddress() != null) {
            userValidator.validateAddressLength(request.getAddress());
            user.setAddress(request.getAddress());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(parseBirthDate(request.getBirthDate()));
        }
    }
}
