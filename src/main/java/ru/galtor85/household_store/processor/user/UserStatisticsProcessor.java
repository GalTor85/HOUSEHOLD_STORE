package ru.galtor85.household_store.processor.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.response.user.UserStatistics;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.i18n.LogMessageService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processor for calculating user statistics.
 *
 * <p>Provides aggregated statistics about users in the system including:
 * <ul>
 *   <li>Total number of users</li>
 *   <li>Active vs inactive users count</li>
 *   <li>Distribution by role (ADMIN, MANAGER, USER)</li>
 * </ul>
 *
 * <p>Statistics are calculated on-demand and reflect the current state
 * of the user database.</p>
 *
 * @author G@LTor85
 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatisticsProcessor {

    private final UserRepository userRepository;
    private final SecurityUserRepository securityUserRepository;
    private final LogMessageService logMsg;

    /**
     * Calculates comprehensive user statistics.
     *
     * <p>Performs the following aggregations:
     * <ol>
     *   <li>Counts total users from UserRepository</li>
     *   <li>Counts active users (where SecurityUser.isEnabled() == true)</li>
     *   <li>Counts users by role (ADMIN, MANAGER, USER)</li>
     *   <li>Calculates inactive users as total - active</li>
     * </ol>
     *
     * @return UserStatistics DTO containing all calculated metrics
     */
    @Transactional(readOnly = true)
    public UserStatistics calculateStatistics() {
        log.debug(logMsg.get("user.statistics.calculation.start"));

        List<User> allUsers = userRepository.findAll();
        List<Long> userIds = extractUserIds(allUsers);
        List<SecurityUser> allSecurityUsers = securityUserRepository.findAllById(userIds);

        Map<Role, Long> roleCounts = countUsersByRole(allSecurityUsers);

        long total = allUsers.size();
        long active = countActiveUsers(allSecurityUsers);
        long inactive = total - active;

        long admins = roleCounts.getOrDefault(Role.ADMIN, 0L);
        long managers = roleCounts.getOrDefault(Role.MANAGER, 0L);
        long regular = roleCounts.getOrDefault(Role.USER, 0L);

        UserStatistics stats = new UserStatistics(total, active, inactive, admins, managers, regular);

        log.info(logMsg.get(
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
     * Extracts user IDs from a list of User entities.
     *
     * @param users list of User entities
     * @return list of user IDs
     */
    private List<Long> extractUserIds(List<User> users) {
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    /**
     * Counts active users from a list of SecurityUser entities.
     *
     * @param securityUsers list of SecurityUser entities
     * @return number of enabled (active) users
     */
    private long countActiveUsers(List<SecurityUser> securityUsers) {
        return securityUsers.stream()
                .filter(SecurityUser::isEnabled)
                .count();
    }

    /**
     * Groups and counts SecurityUser entities by their role.
     *
     * @param securityUsers list of SecurityUser entities
     * @return map of Role to count
     */
    private Map<Role, Long> countUsersByRole(List<SecurityUser> securityUsers) {
        return securityUsers.stream()
                .collect(Collectors.groupingBy(
                        SecurityUser::getRole,
                        Collectors.counting()
                ));
    }
}