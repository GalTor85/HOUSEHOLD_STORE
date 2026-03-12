package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.Role;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.mapper.UserToEntity;
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
@Tag(name = "Admin Users", description = "API for managing users as an admin")
public class AdminRestController {

    private final UserSearchService userSearchService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final AdminUserCreationService adminUserCreationService;
    private final UserDeletedService userDeletedService;
    private final UserToEntity userToEntity;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /**
     * Получение текущего аутентифицированного администратора
     */
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomAuthenticationException(
                    messageService.get("admin-rest-controller.user.not.authenticated"));
        }

        String currentUsername = auth.getName();
        log.debug(messageService.get("admin-rest-controller.log.getting.current.admin", currentUsername));

        return userSearchService.searchUsersByEmailOrMobileNumber(currentUsername)
                .orElseThrow(() -> new UserNotFoundException(
                        messageService.get("admin-rest-controller.error.current.admin.not.found", currentUsername)
                ));
    }

    // ========== ПОЛУЧЕНИЕ СПИСКА ПОЛЬЗОВАТЕЛЕЙ ==========
    @GetMapping
    @Operation(summary = "Get users list",
            description = "Get a list of all users with optional filtering and sorting")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String sort) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.fetching.users",
                    currentAdmin.getEmail(), currentAdmin.getId()));

            List<User> users;

            if (hasSearchCriteria(mobileNumber, email, firstName, lastName)) {
                users = userSearchService.searchUsersByCriteria(
                        mobileNumber, email, firstName, lastName, sort);
                log.debug(messageService.get("admin-rest-controller.log.searching.users.with.criteria",
                        email, mobileNumber, firstName, lastName));
            } else {
                users = userSearchService.getAllUsers(sort);
                log.debug(messageService.get("admin-rest-controller.log.getting.all.users", sort));
            }

            List<UserResponse> userResponses = users.stream()
                    .map(userMapper::build)
                    .collect(Collectors.toList());

            log.info(messageService.get("admin-rest-controller.log.returning.users", userResponses.size()));

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.fetched"),
                    userResponses));

        } catch (RuntimeException e) {
            throw new CustomAuthenticationException(
                    messageService.get("admin-rest-controller.error.authentication", e.getMessage()));
        } catch (Exception e) {
            throw new UserFetchedException(
                    messageService.get("admin-rest-controller.user.fetch.error", e.getMessage()));
        }
    }

    // ========== СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @PostMapping
    @Operation(summary = "Create user",
            description = "Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.creating.user",
                    currentAdmin.getEmail(), request.getEmail()));

            String creator = currentAdmin.getEmail() + " " + currentAdmin.getMobileNumber();
            User newUser = userToEntity.build(request, creator);
            User createdUser = adminUserCreationService.createUserWithRole(
                    currentAdmin, newUser, request.getRole());

            log.info(messageService.get("admin-rest-controller.log.user.created.success", createdUser.getEmail()));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            messageService.get("admin-rest-controller.user.created"),
                            userMapper.build(createdUser)));

        } catch (IllegalArgumentException e) {
            throw new ValidationRequestException(
                    messageService.get("admin-rest-controller.user.create.validation",
                            e.getMessage(), request.getEmail(), request.getMobileNumber()),
                    request.getEmail() + " " + request.getMobileNumber());
        } catch (Exception e) {
            throw new UserCreateException(
                    messageService.get("admin-rest-controller.user.create.error", e.getMessage()));
        }
    }

    // ========== ИЗМЕНЕНИЕ РОЛИ ПОЛЬЗОВАТЕЛЯ ==========
    @PatchMapping("/{userId}/role")
    @Operation(summary = "Update user role",
            description = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.changing.role",
                    currentAdmin.getEmail(), userId, request.getNewRole()));

            User updatedUser = userRoleService.changeUserRole(
                    currentAdmin, userId, request.getNewRole());

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.role.updated"),
                    userMapper.build(updatedUser)));

        } catch (IllegalArgumentException e) {
            throw new ValidationRequestException(
                    messageService.get("admin-rest-controller.user.role.validation",
                            e.getMessage(), userId, request.getNewRole()),
                    request.getNewRole().toString() + " for user ID " + userId);
        } catch (SecurityException e) {
            throw new UserAccessException(
                    messageService.get("admin-rest-controller.user.role.denied", e.getMessage()));
        } catch (Exception e) {
            throw new UserUpdateRoleException(
                    messageService.get("admin-rest-controller.user.role.error", e.getMessage()));
        }
    }

    // ========== ИЗМЕНЕНИЕ СТАТУСА ПОЛЬЗОВАТЕЛЯ ==========
    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status",
            description = "Activate or deactivate user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        try {
            User currentAdmin = getCurrentAdmin();
            String statusText = request.getActive() ?
                    messageService.get("admin-rest-controller.user.status.active") :
                    messageService.get("admin-rest-controller.user.status.inactive");
            log.info(messageService.get("admin-rest-controller.log.admin.changing.status",
                    currentAdmin.getEmail(), userId, statusText));

            User updatedUser = userStatusService.toggleUserActive(
                    currentAdmin, userId, request.getActive());

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.status.updated"),
                    userMapper.build(updatedUser)));

        } catch (IllegalArgumentException e) {
            throw new ValidationRequestException(
                    messageService.get("admin-rest-controller.user.status.validation",
                            e.getMessage(), userId, request.getActive()),
                    request.getActive() + " for user ID " + userId);
        } catch (SecurityException e) {
            throw new UserAccessException(
                    messageService.get("admin-rest-controller.user.status.denied", e.getMessage()));
        } catch (Exception e) {
            throw new UserUpdateStatusException(
                    messageService.get("admin-rest-controller.user.status.error", e.getMessage()));
        }
    }

    // ========== УДАЛЕНИЕ ПОЛЬЗОВАТЕЛЯ ==========
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user",
            description = "Delete user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.deleting.user",
                    currentAdmin.getEmail(), userId));

            if (currentAdmin.getId().equals(userId)) {
                log.warn(messageService.get("admin-rest-controller.log.admin.attempt.delete.self"));
                throw new UserAccessException(messageService.get("admin-rest-controller.user.self.delete"));
            }

            User userToDelete = userSearchService.getUserById(userId);

            if (!currentAdmin.getRole().canManage(userToDelete.getRole())) {
                log.warn(messageService.get("admin-rest-controller.log.admin.insufficient.permissions",
                        currentAdmin.getEmail(), userToDelete.getRole()));
                throw new UserAccessException(
                        messageService.get("admin-rest-controller.user.delete.denied", userToDelete.getRole()));
            }

            userDeletedService.deleteUser(userId);
            log.info(messageService.get("admin-rest-controller.log.user.deleted.success", userId));

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.deleted"),
                    null));

        } catch (IllegalArgumentException e) {
            throw new ValidationRequestException(
                    messageService.get("admin-rest-controller.user.delete.validation", e.getMessage()),
                    getCurrentAdmin().getEmail() + " " + getCurrentAdmin().getMobileNumber());
        } catch (SecurityException | UserAccessException e) {
            throw new UserAccessException(
                    messageService.get("admin-rest-controller.user.delete.denied.general", e.getMessage()));
        }
    }

    // ========== ПОЛУЧЕНИЕ ОДНОГО ПОЛЬЗОВАТЕЛЯ ==========
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID",
            description = "Get detailed information about a user")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long userId) {

        try {
            log.debug(messageService.get("admin-rest-controller.log.getting.user.by.id", userId));

            User user = userSearchService.getUserById(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.found"),
                    userMapper.build(user)));

        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException(
                    messageService.get("admin-rest-controller.user.not.found", userId));
        } catch (Exception e) {
            throw new UserFetchedException(
                    messageService.get("admin-rest-controller.user.fetch.error", e.getMessage()));
        }
    }

    // ========== ПОИСК ПОЛЬЗОВАТЕЛЕЙ ПО EMAIL ИЛИ MOBILENUMBER ==========
    @GetMapping("/search")
    @Operation(summary = "Search users",
            description = "Search users by email or mobile number")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsersByEmailOrMobileNumber(
            @RequestParam String identify,
            @RequestParam(required = false) String sort) {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.searching.users",
                    currentAdmin.getEmail(), identify));

            List<User> users = userSearchService.searchUsersByCriteria(
                    identify, identify, null, null, sort);

            if (users.isEmpty()) {
                throw new UserNotFoundException(
                        messageService.get("admin-rest-controller.user.search.not.found", identify));
            }

            List<UserResponse> userResponses = users.stream()
                    .map(userMapper::build)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.search.results"),
                    userResponses));

        } catch (RuntimeException e) {
            throw new UserNotFoundException(
                    messageService.get("admin-rest-controller.user.search.error", e.getMessage()));
        } catch (Exception e) {
            throw new UserFetchedException(
                    messageService.get("admin-rest-controller.user.search.error", e.getMessage()));
        }
    }

    // ========== ПОЛУЧЕНИЕ СТАТИСТИКИ ==========
    @GetMapping("/stats")
    @Operation(summary = "Get statistics",
            description = "Get system usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {

        try {
            User currentAdmin = getCurrentAdmin();
            log.info(messageService.get("admin-rest-controller.log.admin.getting.stats", currentAdmin.getEmail()));

            List<User> allUsers = userSearchService.getAllUsers(null);

            Map<String, Object> stats = new HashMap<>();
            stats.put(messageService.get("admin-rest-controller.stats.total"), allUsers.size());
            stats.put(messageService.get("admin-rest-controller.stats.active"),
                    allUsers.stream().filter(User::isActive).count());
            stats.put(messageService.get("admin-rest-controller.stats.inactive"),
                    allUsers.stream().filter(u -> !u.isActive()).count());
            stats.put(messageService.get("admin-rest-controller.stats.admins"),
                    allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count());
            stats.put(messageService.get("admin-rest-controller.stats.managers"),
                    allUsers.stream().filter(u -> u.getRole() == Role.MANAGER).count());
            stats.put(messageService.get("admin-rest-controller.stats.users"),
                    allUsers.stream().filter(u -> u.getRole() == Role.USER).count());
            stats.put(messageService.get("admin-rest-controller.stats.logged"),
                    allUsers.stream().filter(u -> u.getCreator() != null).count());

            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.stats.fetched"),
                    stats));

        } catch (Exception e) {
            throw new StatisticException(
                    messageService.get("admin-rest-controller.stats.error", e.getMessage()));
        }
    }

    private boolean hasSearchCriteria(String mobileNumber, String email,
                                      String firstName, String lastName) {
        return (mobileNumber != null && !mobileNumber.trim().isEmpty()) ||
                (email != null && !email.trim().isEmpty()) ||
                (firstName != null && !firstName.trim().isEmpty()) ||
                (lastName != null && !lastName.trim().isEmpty());
    }
}