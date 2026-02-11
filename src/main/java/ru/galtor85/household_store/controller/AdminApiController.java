package ru.galtor85.household_store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final UserSearchService userSearchService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final AdminUserCreationService adminUserCreationService;
    private final UserDeletedService userDeletedService;

    /**
     * Получение текущего аутентифицированного администратора
     */
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Пользователь не аутентифицирован");
        }

        String currentUsername = auth.getName();
        log.debug("Getting current admin by username: {}", currentUsername);

        return userSearchService.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("Текущий администратор не найден: " + currentUsername));
    }

    // ========== ПОЛУЧЕНИЕ СПИСКА ПОЛЬЗОВАТЕЛЕЙ ==========
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String search) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} (ID: {}) fetching users",
                    currentAdmin.getEmail(), currentAdmin.getId());

            List<User> users;
            if (search != null && !search.trim().isEmpty()) {
                users = userSearchService.searchUsers(search.trim());
                log.debug("Searching users with query: {}", search);
            } else {
                users = userSearchService.getAllUsers(currentAdmin);
                log.debug("Getting all users for admin");
            }

            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::fromEntity)
                    .collect(Collectors.toList());

            log.info("Returning {} users", userResponses.size());
            return ResponseEntity.ok(
                    ApiResponse.success("Пользователи получены", userResponses));

        } catch (RuntimeException e) {
            log.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Ошибка аутентификации: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при получении пользователей: " + e.getMessage()));
        }
    }

    // ========== СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} creating new user with email: {}",
                    currentAdmin.getEmail(), request.getEmail());

            User newUser = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .surname(request.getSurname())
                    .role(request.getRole())
                    .active(request.isActive())
                    .build();

            User createdUser = adminUserCreationService.createUserWithRole(
                    currentAdmin, newUser, request.getRole());

            log.info("User created successfully: {}", createdUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Пользователь создан",
                            UserResponse.fromEntity(createdUser)));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при создании пользователя: " + e.getMessage()));
        }
    }

    // ========== ИЗМЕНЕНИЕ РОЛИ ПОЛЬЗОВАТЕЛЯ ==========
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} changing role for user ID: {} to {}",
                    currentAdmin.getEmail(), userId, request.getNewRole());

            User updatedUser = userRoleService.changeUserRole(
                    currentAdmin, userId, request.getNewRole());

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Роль пользователя изменена",
                            UserResponse.fromEntity(updatedUser)));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error changing role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Permission denied changing role: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing role: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при изменении роли: " + e.getMessage()));
        }
    }

    // ========== ИЗМЕНЕНИЕ СТАТУСА ПОЛЬЗОВАТЕЛЯ ==========
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} changing status for user ID: {} to {}",
                    currentAdmin.getEmail(), userId, request.isActive() ? "active" : "inactive");

            User updatedUser = userStatusService.toggleUserActive(
                    currentAdmin, userId, request.isActive());

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Статус пользователя изменен",
                            UserResponse.fromEntity(updatedUser)));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error changing status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Permission denied changing status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при изменении статуса: " + e.getMessage()));
        }
    }

    // ========== УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} deleting user ID: {}", currentAdmin.getEmail(), userId);

            // Проверяем нельзя ли удалить самого себя
            if (currentAdmin.getId().equals(userId)) {
                log.warn("Admin attempted to delete their own account");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Нельзя удалить свой собственный аккаунт"));
            }

            User userToDelete = userSearchService.getUserById(userId);

            // Проверяем права на удаление
            if (!currentAdmin.getRole().canManage(userToDelete.getRole())) {
                log.warn("Admin {} insufficient permissions to delete user with role {}",
                        currentAdmin.getEmail(), userToDelete.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                "Недостаточно прав для удаления пользователя с ролью " +
                                        userToDelete.getRole()));
            }

            userDeletedService.deleteUser(userId);
            log.info("User ID: {} deleted successfully", userId);

            return ResponseEntity.ok(
                    ApiResponse.success("Пользователь успешно удален", null));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Permission denied deleting user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при удалении пользователя: " + e.getMessage()));
        }
    }

    // ========== ПОЛУЧЕНИЕ ОДНОГО ПОЛЬЗОВАТЕЛЯ ==========
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        try {
            log.debug("Getting user by ID: {}", userId);
            User user = userSearchService.getUserById(userId);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Пользователь найден",
                            UserResponse.fromEntity(user)));

        } catch (IllegalArgumentException e) {
            log.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при получении пользователя: " + e.getMessage()));
        }
    }

    // ========== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ПО EMAIL ==========
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsersByEmail(
            @RequestParam String email) {
        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} searching users by email: {}",
                    currentAdmin.getEmail(), email);

            List<User> users = userSearchService.searchUsers(email);
            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success("Результаты поиска", userResponses));

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при поиске пользователей: " + e.getMessage()));
        }
    }

    // ========== ПОЛУЧЕНИЕ СТАТИСТИКИ ==========
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            User currentAdmin = getCurrentAdmin();
            log.info("Admin {} fetching user statistics", currentAdmin.getEmail());

            List<User> allUsers = userSearchService.getAllUsers(currentAdmin);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", allUsers.size());
            stats.put("activeUsers", allUsers.stream().filter(User::isActive).count());
            stats.put("admins", allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count());
            stats.put("managers", allUsers.stream().filter(u -> u.getRole() == Role.MANAGER).count());
            stats.put("regularUsers", allUsers.stream().filter(u -> u.getRole() == Role.USER).count());

            return ResponseEntity.ok(
                    ApiResponse.success("Статистика пользователей", stats));

        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка при получении статистики: " + e.getMessage()));
        }
    }
}