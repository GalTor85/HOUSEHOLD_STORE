package ru.galtor85.household_store.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.config.FinancialConfig;
import ru.galtor85.household_store.dto.response.finance.PriceCalculationResult;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.entity.user.UserType;
import ru.galtor85.household_store.processor.user.UserTypeDiscountProcessor;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.repository.user.UserTypeAssignmentRepository;
import ru.galtor85.household_store.service.user.UserTypeAssignmentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User Type Discount Processor Tests")
class UserTypeDiscountProcessorTest {

    @Autowired
    private UserTypeDiscountProcessor processor;

    @Autowired
    private UserTypeAssignmentService userTypeAssignmentService;

    @Autowired
    private UserTypeAssignmentRepository userTypeAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialConfig financialConfig;

    private List<PriceCalculationResult.AppliedDiscount> appliedDiscounts;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        appliedDiscounts = new ArrayList<>();

        User user = User.builder()
                .email("test-" + System.currentTimeMillis() + "@example.com")
                .firstName("Test")
                .lastName("User")
                .mobileNumber("+79991234567")
                .creator("test")
                .build();
        User saved = userRepository.save(user);
        testUserId = saved.getId();

        userTypeAssignmentService.assignUserType(testUserId, UserType.RETAIL, "test", "test");
    }

    private void setUserType(UserType userType) {
        userTypeAssignmentRepository.findActiveByUserId(testUserId)
                .ifPresent(assignment -> {
                    assignment.setActive(false);
                    userTypeAssignmentRepository.save(assignment);
                });
        userTypeAssignmentService.assignUserType(testUserId, userType, "test", "test");
    }

    // TEST-01: No discount for RETAIL user
    @Test
    @DisplayName("TEST-01: Should return no discount for RETAIL user")
    void testNoDiscountForRetailUser() {
        BigDecimal total = BigDecimal.valueOf(1000);

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(total);
        assertThat(result.userType()).isEqualTo(UserType.RETAIL);
        assertThat(result.appliedPercent()).isEqualTo(0.0);
        assertThat(appliedDiscounts).isEmpty();
    }

    // TEST-02: Wholesale discount
    @Test
    @DisplayName("TEST-02: Should apply wholesale discount correctly")
    void testWholesaleDiscount() {
        setUserType(UserType.WHOLESALE);
        BigDecimal total = BigDecimal.valueOf(1000);
        double expectedPercent = financialConfig.getDiscounts().getWholesale();

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        BigDecimal expectedDiscount = total.multiply(BigDecimal.valueOf(expectedPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = total.subtract(expectedDiscount);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.userType()).isEqualTo(UserType.WHOLESALE);
        assertThat(result.appliedPercent()).isEqualTo(expectedPercent);
        assertThat(appliedDiscounts).hasSize(1);
    }

    // TEST-03: VIP discount
    @Test
    @DisplayName("TEST-03: Should apply VIP discount correctly")
    void testVipDiscount() {
        setUserType(UserType.VIP);
        BigDecimal total = BigDecimal.valueOf(1000);
        double expectedPercent = financialConfig.getDiscounts().getVip();

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        BigDecimal expectedDiscount = total.multiply(BigDecimal.valueOf(expectedPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = total.subtract(expectedDiscount);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.userType()).isEqualTo(UserType.VIP);
        assertThat(result.appliedPercent()).isEqualTo(expectedPercent);
        assertThat(appliedDiscounts).hasSize(1);
    }

    // TEST-04: Partner discount
    @Test
    @DisplayName("TEST-04: Should apply partner discount correctly")
    void testPartnerDiscount() {
        setUserType(UserType.PARTNER);
        BigDecimal total = BigDecimal.valueOf(1000);
        double expectedPercent = financialConfig.getDiscounts().getPartner();

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        BigDecimal expectedDiscount = total.multiply(BigDecimal.valueOf(expectedPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = total.subtract(expectedDiscount);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.userType()).isEqualTo(UserType.PARTNER);
        assertThat(result.appliedPercent()).isEqualTo(expectedPercent);
        assertThat(appliedDiscounts).hasSize(1);
    }

    // TEST-05: Employee discount
    @Test
    @DisplayName("TEST-05: Should apply employee discount correctly")
    void testEmployeeDiscount() {
        setUserType(UserType.EMPLOYEE);
        BigDecimal total = BigDecimal.valueOf(1000);
        double expectedPercent = financialConfig.getDiscounts().getEmployee();

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        BigDecimal expectedDiscount = total.multiply(BigDecimal.valueOf(expectedPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = total.subtract(expectedDiscount);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.userType()).isEqualTo(UserType.EMPLOYEE);
        assertThat(result.appliedPercent()).isEqualTo(expectedPercent);
        assertThat(appliedDiscounts).hasSize(1);
    }

    // TEST-06: No user type found (default to RETAIL)
    @Test
    @DisplayName("TEST-06: Should default to RETAIL when no user type found")
    void testNoUserTypeFound() {
        userTypeAssignmentRepository.findActiveByUserId(testUserId)
                .ifPresent(assignment -> {
                    assignment.setActive(false);
                    userTypeAssignmentRepository.save(assignment);
                });

        BigDecimal total = BigDecimal.valueOf(1000);

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        // Processor defaults to RETAIL when no user type found
        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(total);
        assertThat(result.userType()).isEqualTo(UserType.RETAIL);  // ← ожидаем RETAIL
        assertThat(result.appliedPercent()).isEqualTo(0.0);
        assertThat(appliedDiscounts).isEmpty();
    }

    // TEST-07: Zero total
    @Test
    @DisplayName("TEST-07: Should handle zero total correctly")
    void testZeroTotal() {
        setUserType(UserType.VIP);
        BigDecimal total = BigDecimal.ZERO;

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.userType()).isNull();
        assertThat(result.appliedPercent()).isEqualTo(0.0);
        assertThat(appliedDiscounts).isEmpty();
    }

    // TEST-08: Discount rounding
    @Test
    @DisplayName("TEST-08: Should round discount correctly (2 decimal places)")
    void testDiscountRounding() {
        setUserType(UserType.VIP);
        BigDecimal total = BigDecimal.valueOf(999.99);

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        BigDecimal expectedDiscount = BigDecimal.valueOf(100.00);
        BigDecimal expectedTotal = BigDecimal.valueOf(899.99);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(expectedTotal);
        assertThat(result.appliedPercent()).isEqualTo(10.0);
        assertThat(appliedDiscounts.getFirst().getDiscountAmount()).isEqualByComparingTo(expectedDiscount);
    }

    // TEST-09: Verify discount added to appliedDiscounts list
    @Test
    @DisplayName("TEST-09: Should add discount to appliedDiscounts list")
    void testDiscountAddedToList() {
        setUserType(UserType.VIP);
        BigDecimal total = BigDecimal.valueOf(1000);

        processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        assertThat(appliedDiscounts).hasSize(1);
        assertThat(appliedDiscounts.getFirst().getType()).isEqualTo("USER_TYPE");
        assertThat(appliedDiscounts.getFirst().getDiscountAmount()).isPositive();
    }

    // TEST-10: Corporate user (no discount)
    @Test
    @DisplayName("TEST-10: Should return no discount for CORPORATE user")
    void testNoDiscountForCorporateUser() {
        setUserType(UserType.CORPORATE);
        BigDecimal total = BigDecimal.valueOf(1000);

        UserTypeDiscountProcessor.UserTypeDiscountResult result =
                processor.applyUserTypeDiscount(total, testUserId, appliedDiscounts);

        assertThat(result.totalAfterDiscount()).isEqualByComparingTo(total);
        assertThat(result.userType()).isEqualTo(UserType.CORPORATE);
        assertThat(result.appliedPercent()).isEqualTo(0.0);
        assertThat(appliedDiscounts).isEmpty();
    }
}