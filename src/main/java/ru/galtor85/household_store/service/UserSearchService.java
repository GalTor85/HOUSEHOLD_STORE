package ru.galtor85.household_store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.advice.exception.UserNotFoundException;
import ru.galtor85.household_store.dto.UserStatistics;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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
                                            String sort, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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
    public Optional<User> searchUsersByEmailOrMobileNumber(String identify, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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

    /**
     * @deprecated Роль теперь хранится в SecurityUser. Используйте SecurityUserRepository напрямую.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(Role role, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();
        log.warn("Method findUsersByRole is deprecated - role is now in SecurityUser");
        return List.of();
    }

    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        boolean exists = userRepository.existsByEmail(email);

        log.debug(messageService.get(
                "user-search-service.log.user.exists.email",
                email,
                exists
        ));

        return exists;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

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

    public User getUserById(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        final Long finalUserId = userId;
        final Locale finalLocale = locale;

        return userRepository.findById(finalUserId)
                .orElseThrow(() -> {
                    String errorMessage = messageService.get(
                            "user-search-service.error.user.not.found.id",
                            finalUserId
                    );
                    log.error(errorMessage);
                    return new UserNotFoundException(errorMessage);
                });
    }

    @Transactional(readOnly = true)
    public boolean userExistsById(Long userId, Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        boolean exists = userRepository.existsById(userId);

        log.debug(messageService.get(
                "user-search-service.log.user.exists.id",
                userId,
                exists
        ));

        return exists;
    }

    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics(Locale locale) {
        locale = locale != null ? locale : Locale.getDefault();

        List<User> allUsers = userRepository.findAll();

        List<Long> userIds = allUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<SecurityUser> allSecurityUsers = securityUserRepository.findAllById(userIds);

        Map<Long, SecurityUser> securityUserMap = allSecurityUsers.stream()
                .collect(Collectors.toMap(SecurityUser::getId, su -> su));

        long total = allUsers.size();
        long active = allSecurityUsers.stream()
                .filter(SecurityUser::isEnabled)
                .count();
        long inactive = total - active;

        long admins = allSecurityUsers.stream()
                .filter(su -> su.getRole() == Role.ADMIN)
                .count();
        long managers = allSecurityUsers.stream()
                .filter(su -> su.getRole() == Role.MANAGER)
                .count();
        long regular = allSecurityUsers.stream()
                .filter(su -> su.getRole() == Role.USER)
                .count();

        UserStatistics stats = new UserStatistics(total, active, inactive, admins, managers, regular);

        log.info(messageService.get(
                "user-search-service.log.user.statistics",
                total,
                active,
                admins,
                managers,
                regular
        ));

        return stats;
    }

    /**
     * Получить SecurityUser по ID пользователя
     */
    public SecurityUser getSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        messageService.get("user-search-service.error.security.user.not.found", userId)
                ));
    }

    /**
     * Получить SecurityUser по ID пользователя (с опциональным результатом)
     */
    public Optional<SecurityUser> findSecurityUserByUserId(Long userId) {
        return securityUserRepository.findById(userId);
    }

    // ========== ПЕРЕГРУЖЕННЫЕ МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        return getAllUsers(sort, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber, String email,
                                            String firstName, String lastName,
                                            String sort) {
        return searchUsersByCriteria(mobileNumber, email, firstName, lastName, sort, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public Optional<User> searchUsersByEmailOrMobileNumber(String identify) {
        return searchUsersByEmailOrMobileNumber(identify, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByRole(Role role) {
        return findUsersByRole(role, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email) {
        return userExistsByEmail(email, Locale.getDefault());
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return findByEmail(email, Locale.getDefault());
    }

    public User getUserById(Long userId) {
        return getUserById(userId, Locale.getDefault());
    }
}

