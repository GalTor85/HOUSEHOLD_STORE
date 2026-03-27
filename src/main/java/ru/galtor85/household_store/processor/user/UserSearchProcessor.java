package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.i18n.MessageService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSearchProcessor {

    private final UserRepository userRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        String sortField = sort != null ? sort.trim() : "id";
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, sortField));

        log.debug(messageService.get(
                "user-search-service.log.user.search.all",
                users.size(),
                sortField
        ));

        return users;
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber, String email,
                                            String firstName, String lastName,
                                            String sort) {
        String trimmedEmail = email != null ? email.trim() : null;
        String trimmedMobile = mobileNumber != null ? mobileNumber.trim() : null;
        String trimmedFirstName = firstName != null ? firstName.trim() : null;
        String trimmedLastName = lastName != null ? lastName.trim() : null;
        String sortField = sort != null ? sort.trim() : "id";

        List<User> users = userRepository.findByEmailContainingOrMobileNumberContainingOrFirstNameContainingOrLastNameContaining(
                trimmedEmail, trimmedMobile, trimmedFirstName, trimmedLastName,
                Sort.by(Sort.Direction.ASC, sortField));

        log.debug(messageService.get(
                "user-search-service.log.user.search.criteria",
                users.size(),
                trimmedEmail,
                trimmedMobile,
                trimmedFirstName,
                trimmedLastName
        ));

        return users;
    }

    @Transactional(readOnly = true)
    public Optional<User> searchUsersByEmailOrMobileNumber(String identify) {
        Optional<User> user = userRepository.findByEmailOrMobileNumber(identify, identify);

        if (user.isPresent()) {
            log.debug(messageService.get(
                    "user-search-service.log.user.search.identify.found",
                    identify,
                    user.get().getEmail()
            ));
        } else {
            log.debug(messageService.get(
                    "user-search-service.log.user.search.identify.not.found",
                    identify
            ));
        }

        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            log.debug(messageService.get(
                    "user-search-service.log.user.find.email.found",
                    email
            ));
        } else {
            log.debug(messageService.get(
                    "user-search-service.log.user.find.email.not.found",
                    email
            ));
        }

        return user;
    }

    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email) {
        boolean exists = userRepository.existsByEmail(email);

        log.debug(messageService.get(
                "user-search-service.log.user.exists.email",
                email,
                exists
        ));

        return exists;
    }

    @Transactional(readOnly = true)
    public boolean userExistsById(Long userId) {
        boolean exists = userRepository.existsById(userId);

        log.debug(messageService.get(
                "user-search-service.log.user.exists.id",
                userId,
                exists
        ));

        return exists;
    }
}