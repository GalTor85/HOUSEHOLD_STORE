package ru.galtor85.household_store.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserStatistics;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.processor.security.SecurityUserProcessor;
import ru.galtor85.household_store.processor.user.UserSearchProcessor;
import ru.galtor85.household_store.processor.user.UserStatisticsProcessor;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.validator.common.SortFieldValidator;
import ru.galtor85.household_store.validator.auth.UserSearchValidator;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchService {

    // Валидаторы
    private final UserSearchValidator validator;

    // Процессоры
    private final UserSearchProcessor searchProcessor;
    private final UserStatisticsProcessor statisticsProcessor;
    private final SecurityUserProcessor securityUserProcessor;

    // Утилиты
    private final SortFieldValidator sortFieldValidator;

    // ========== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ==========

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String sort) {
        String validSort = sortFieldValidator.validateAndGetSortField(sort);
        return searchProcessor.getAllUsers(validSort);
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersByCriteria(String mobileNumber, String email,
                                            String firstName, String lastName,
                                            String sort) {
        String validSort = sortFieldValidator.validateAndGetSortField(sort);
        return searchProcessor.searchUsersByCriteria(
                mobileNumber, email, firstName, lastName, validSort);
    }

    @Transactional(readOnly = true)
    public Optional<User> searchUsersByEmailOrMobileNumber(String identify) {
        return searchProcessor.searchUsersByEmailOrMobileNumber(identify);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return searchProcessor.findByEmail(email);
    }

    // ========== ПРОВЕРКА СУЩЕСТВОВАНИЯ ==========

    @Transactional(readOnly = true)
    public boolean userExistsByEmail(String email) {
        return searchProcessor.userExistsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean userExistsById(Long userId) {
        return searchProcessor.userExistsById(userId);
    }

    // ========== ПОЛУЧЕНИЕ ПОЛЬЗОВАТЕЛЯ ПО ID ==========

    public User getUserById(Long userId) {
        return validator.validateUserExists(userId);
    }

    // ========== СТАТИСТИКА ==========

    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        return statisticsProcessor.calculateStatistics();
    }

    // ========== SECURITY USER ==========

    public SecurityUser getSecurityUserByUserId(Long userId) {
        return securityUserProcessor.getSecurityUserByUserId(userId);
    }

    public Optional<SecurityUser> findSecurityUserByUserId(Long userId) {
        return securityUserProcessor.findSecurityUserByUserId(userId);
    }

    // ========== ПЕРЕГРУЖЕННЫЕ МЕТОДЫ (БЕЗ LOCALE) ==========


}

