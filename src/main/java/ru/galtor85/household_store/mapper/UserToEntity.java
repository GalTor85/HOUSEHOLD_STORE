package ru.galtor85.household_store.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.galtor85.household_store.dto.UserCreateRequest;
import ru.galtor85.household_store.dto.UserEditRequest;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.MessageService;

import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserToEntity {

    private final MessageService messageService;

    public User build(UserCreateRequest request, String creator, Locale locale) {
        if (request == null) {
            log.warn(messageService.get("user-to-entity.log.mapper.request.null"));
            return null;
        }

        locale = locale != null ? locale : Locale.getDefault();

        log.debug(messageService.get("user-to-entity.log.mapper.converting.user", request.getEmail()));

        return User.builder()
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .active(request.getActive() != null ? request.getActive() : true)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .surname(request.getSurname())
                .password(request.getPassword())
                .birthDate(parseBirthDate(request.getBirthDate(), locale))
                .address(request.getAddress())
                .creator(creator)
                .build();
    }

    public User build(UserCreateRequest request, String creator) {
        return build(request, creator, Locale.getDefault());
    }

    private LocalDate parseBirthDate(String birthDate, Locale locale) {
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

    public void updateUserFromRequest(User user, UserEditRequest request, Locale locale) {
        if (user == null || request == null) {
            return;
        }

        log.debug(messageService.get("user-to-entity.log.mapper.updating.user", user.getId()));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getMobileNumber() != null) {
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getSurname() != null) {
            user.setSurname(request.getSurname());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(parseBirthDate(request.getBirthDate(), locale));
        }
    }
}
