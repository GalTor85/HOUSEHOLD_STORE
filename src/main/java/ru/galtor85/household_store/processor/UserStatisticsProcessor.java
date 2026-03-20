package ru.galtor85.household_store.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.UserStatistics;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.repository.SecurityUserRepository;
import ru.galtor85.household_store.repository.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatisticsProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public UserStatistics calculateStatistics() {
        List<User> allUsers = userRepository.findAll();

        List<Long> userIds = allUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<SecurityUser> allSecurityUsers = securityUserRepository.findAllById(userIds);

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
}