package ru.galtor85.household_store.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.dto.request.auth.LoginFormRequest;
import ru.galtor85.household_store.dto.request.cleanup.SoftDeleteRequest;
import ru.galtor85.household_store.entity.user.Role;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.auth.SecurityUserRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.security.SecurityUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Security Access Tests")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityUserRepository securityUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        log.info("=== SETUP: Creating test users ===");

        // Create Admin user
        User adminUser = User.builder()
                .email("admin-test-" + System.currentTimeMillis() + "@example.com")
                .firstName("Admin")
                .lastName("Test")
                .mobileNumber("+79990000001")
                .creator("system")
                .build();
        adminUser = userRepository.save(adminUser);

        SecurityUser adminSecurity = SecurityUser.builder()
                .userId(adminUser.getId())
                .password(passwordEncoder.encode("Admin123!"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        securityUserRepository.save(adminSecurity);
        adminToken = loginAndGetToken(adminUser.getEmail(), "Admin123!");

        // Create Manager user
        User managerUser = User.builder()
                .email("manager-test-" + System.currentTimeMillis() + "@example.com")
                .firstName("Manager")
                .lastName("Test")
                .mobileNumber("+79990000002")
                .creator("system")
                .build();
        managerUser = userRepository.save(managerUser);

        SecurityUser managerSecurity = SecurityUser.builder()
                .userId(managerUser.getId())
                .password(passwordEncoder.encode("Manager123!"))
                .role(Role.MANAGER)
                .active(true)
                .build();
        securityUserRepository.save(managerSecurity);
        managerToken = loginAndGetToken(managerUser.getEmail(), "Manager123!");

        // Create Regular User
        User regularUser = User.builder()
                .email("user-test-" + System.currentTimeMillis() + "@example.com")
                .firstName("User")
                .lastName("Test")
                .mobileNumber("+79990000003")
                .creator("system")
                .build();
        regularUser = userRepository.save(regularUser);

        SecurityUser userSecurity = SecurityUser.builder()
                .userId(regularUser.getId())
                .password(passwordEncoder.encode("User123!"))
                .role(Role.USER)
                .active(true)
                .build();
        securityUserRepository.save(userSecurity);
        userToken = loginAndGetToken(regularUser.getEmail(), "User123!");

        log.info("=== SETUP COMPLETE ===");
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        LoginFormRequest loginRequest = new LoginFormRequest();
        loginRequest.setEmail(identifier);
        loginRequest.setPassword(password);

        String response = mockMvc.perform(post("/app/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        if (jsonNode.has("data") && jsonNode.get("data").has("accessToken")) {
            return jsonNode.get("data").get("accessToken").asText();
        }
        return null;
    }

    // =========================================================================
    // PUBLIC ENDPOINTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Root endpoint should redirect to /app/")
    void testRootEndpointRedirect() throws Exception {
        mockMvc.perform(get("/app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/app/"));
    }

    @Test
    @Order(2)
    @DisplayName("Root endpoint with slash should return 200")
    void testRootEndpointWithSlash() throws Exception {
        mockMvc.perform(get("/app/"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("Health endpoint should be accessible without authentication")
    void testHealthEndpointAccessible() throws Exception {
        mockMvc.perform(get("/app/health"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("Info endpoint should be accessible without authentication")
    void testInfoEndpointAccessible() throws Exception {
        mockMvc.perform(get("/app/info"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("Ping endpoint should be accessible without authentication")
    void testPingEndpointAccessible() throws Exception {
        mockMvc.perform(get("/app/ping"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // UNAUTHENTICATED ACCESS
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("User endpoints return 401 without token")
    void testUserEndpointsUnauthenticated() throws Exception {
        mockMvc.perform(get("/app/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("Manager endpoints return 401 without token")
    void testManagerEndpointsUnauthenticated() throws Exception {
        mockMvc.perform(get("/app/manager/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Admin endpoints return 401 without token")
    void testAdminEndpointsUnauthenticated() throws Exception {
        mockMvc.perform(get("/app/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("Cleanup endpoints return 401 without token")
    void testCleanupEndpointsUnauthenticated() throws Exception {
        SoftDeleteRequest request = SoftDeleteRequest.builder().reason("Test").build();
        mockMvc.perform(delete("/app/admin/cleanup/sales-orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // USER ROLE ACCESS
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("USER can access user endpoints")
    void testUserCanAccessUserEndpoints() throws Exception {
        mockMvc.perform(get("/app/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("USER cannot access manager endpoints (403)")
    void testUserCannotAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/app/manager/products")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("USER cannot access admin endpoints (403)")
    void testUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @DisplayName("USER cannot access cleanup endpoints (403)")
    void testUserCannotAccessCleanupEndpoints() throws Exception {
        SoftDeleteRequest request = SoftDeleteRequest.builder().reason("Test").build();
        mockMvc.perform(delete("/app/admin/cleanup/sales-orders/1")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // MANAGER ROLE ACCESS
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("MANAGER can access user endpoints")
    void testManagerCanAccessUserEndpoints() throws Exception {
        mockMvc.perform(get("/app/users/me")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @DisplayName("MANAGER can access manager endpoints")
    void testManagerCanAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/app/manager/products")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("MANAGER cannot access admin endpoints (403)")
    void testManagerCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(17)
    @DisplayName("MANAGER cannot access cleanup endpoints (403)")
    void testManagerCannotAccessCleanupEndpoints() throws Exception {
        SoftDeleteRequest request = SoftDeleteRequest.builder().reason("Test").build();
        mockMvc.perform(delete("/app/admin/cleanup/sales-orders/1")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // ADMIN ROLE ACCESS
    // =========================================================================

    @Test
    @Order(18)
    @DisplayName("ADMIN can access user endpoints")
    void testAdminCanAccessUserEndpoints() throws Exception {
        mockMvc.perform(get("/app/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(19)
    @DisplayName("ADMIN can access manager endpoints")
    void testAdminCanAccessManagerEndpoints() throws Exception {
        mockMvc.perform(get("/app/manager/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(20)
    @DisplayName("ADMIN can access admin endpoints")
    void testAdminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/app/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @DisplayName("ADMIN can access cleanup endpoints (access granted)")
    void testAdminCanAccessCleanupEndpoints() throws Exception {
        SoftDeleteRequest request = SoftDeleteRequest.builder().reason("Test").build();

        // Проверяем, что статус НЕ 403 (доступ разрешен)
        mockMvc.perform(delete("/app/admin/cleanup/sales-orders/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "Access should be allowed for ADMIN, but got 403";
                });
    }

    // =========================================================================
    // INVALID TOKEN
    // =========================================================================

    @Test
    @Order(22)
    @DisplayName("Invalid token returns 401")
    void testInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/app/users/me")
                        .header("Authorization", "Bearer invalid-token-12345"))
                .andExpect(status().isUnauthorized());
    }
}