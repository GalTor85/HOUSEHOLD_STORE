package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.user.UserDeletedService;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("User Delete Tests")
class UserDeleteServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUserRepository securityUserRepository;

    @Autowired
    private UserTypeAssignmentRepository userTypeAssignmentRepository;

    @Autowired
    private UserTypeAssignmentService userTypeAssignmentService;

    @Autowired
    private UserDeletedService userDeletedService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        log.info("=== SETUP: Creating test data ===");

        // Create admin user
        adminUser = User.builder()
                .email("admin-delete-" + System.currentTimeMillis() + "@example.com")
                .firstName("Admin")
                .lastName("Delete")
                .mobileNumber("+79990000000")
                .creator("system")
                .build();
        adminUser = userRepository.save(adminUser);
        log.info("Created admin user with ID: {}", adminUser.getId());

        // Create SecurityUser for admin
        SecurityUser adminSecurity = SecurityUser.builder()
                .userId(adminUser.getId())
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        securityUserRepository.save(adminSecurity);
        log.info("Created admin security user");

        // Create test user to delete
        User testUser = User.builder()
                .email("test-delete-" + System.currentTimeMillis() + "@example.com")
                .firstName("Test")
                .lastName("Delete")
                .mobileNumber("+79991234567")
                .creator("test")
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
        log.info("Created test user with ID: {}", testUserId);

        // Create SecurityUser for test user
        SecurityUser testSecurity = SecurityUser.builder()
                .userId(testUserId)
                .password(passwordEncoder.encode("test123"))
                .role(Role.USER)
                .active(true)
                .build();
        securityUserRepository.save(testSecurity);
        log.info("Created test security user");

        // Create UserTypeAssignment for test user
        userTypeAssignmentService.assignUserType(testUserId, UserType.RETAIL, "test", "test");
        log.info("Created user type assignment");

        log.info("=== SETUP COMPLETE ===");
    }

    @Test
    @DisplayName("TEST-01: Delete user via service - user should be deleted")
    void testDeleteUser() {
        log.info("=== TEST-01 START ===");

        assertThat(userRepository.existsById(testUserId)).isTrue();

        userDeletedService.deleteUserWithCheck(testUserId, adminUser);

        assertThat(userRepository.existsById(testUserId)).isFalse();

        log.info("=== TEST-01 PASSED ===");
    }

    @Test
    @DisplayName("TEST-02: Delete user via service - security user should be deleted")
    void testDeleteUserSecurityUserDeleted() {
        log.info("=== TEST-02 START ===");

        assertThat(securityUserRepository.findByUserId(testUserId)).isPresent();

        userDeletedService.deleteUserWithCheck(testUserId, adminUser);

        assertThat(securityUserRepository.findByUserId(testUserId)).isEmpty();

        log.info("=== TEST-02 PASSED ===");
    }

    @Test
    @DisplayName("TEST-03: Delete user via service - user type assignments should be deleted")
    void testDeleteUserUserTypeAssignmentsDeleted() {
        log.info("=== TEST-03 START ===");

        assertThat(userTypeAssignmentRepository.findActiveByUserId(testUserId)).isPresent();

        userDeletedService.deleteUserWithCheck(testUserId, adminUser);

        assertThat(userTypeAssignmentRepository.findActiveByUserId(testUserId)).isEmpty();

        log.info("=== TEST-03 PASSED ===");
    }

    @Test
    @DisplayName("TEST-04: Delete user via service - verify all related data deleted")
    void testDeleteUserAllRelatedDataDeleted() {
        log.info("=== TEST-04 START ===");

        assertThat(userRepository.existsById(testUserId)).isTrue();
        assertThat(securityUserRepository.findByUserId(testUserId)).isPresent();
        assertThat(userTypeAssignmentRepository.findActiveByUserId(testUserId)).isPresent();

        userDeletedService.deleteUserWithCheck(testUserId, adminUser);

        assertThat(userRepository.existsById(testUserId)).isFalse();
        assertThat(securityUserRepository.findByUserId(testUserId)).isEmpty();
        assertThat(userTypeAssignmentRepository.findActiveByUserId(testUserId)).isEmpty();

        log.info("=== TEST-04 PASSED ===");
    }
}