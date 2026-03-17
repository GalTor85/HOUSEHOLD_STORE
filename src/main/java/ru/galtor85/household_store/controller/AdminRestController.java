package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.mapper.UserToEntity;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SecurityRequirement(name = "Bearer Authentication")
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

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (SecurityUser) auth.getPrincipal();
    }

    private User getCurrentAdmin() {
        SecurityUser securityUser = getCurrentSecurityUser();
        log.debug(messageService.get("admin-rest-controller.log.getting.current.admin",
                securityUser.getUsername()));
        return userSearchService.getUserById(securityUser.getUserId());
    }

    @GetMapping
    @Operation(summary = "Get users list",
            description = "Get a list of all users with optional filtering and sorting")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String sort) {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.fetching.users",
                currentAdmin.getEmail(), currentAdmin.getId()));

        List<User> users;

        if (hasSearchCriteria(mobileNumber, email, firstName, lastName)) {
            users = userSearchService.searchUsersByCriteria(mobileNumber, email, firstName, lastName, sort);
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
    }

    @PostMapping
    @Operation(summary = "Create user",
            description = "Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.creating.user",
                currentAdmin.getEmail(), request.getEmail()));

        String creator = currentAdmin.getEmail() + " " + currentAdmin.getMobileNumber();
        User newUser = userToEntity.build(request, creator);

        User createdUser = adminUserCreationService.createUserWithRole(
                currentAdmin, newUser, request.getPassword(), request.getRole(), null);

        log.info(messageService.get("admin-rest-controller.log.user.created.success", createdUser.getEmail()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("admin-rest-controller.user.created"),
                        userMapper.build(createdUser)));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Update user role",
            description = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.changing.role",
                currentAdmin.getEmail(), userId, request.getNewRole()));

        User updatedUser = userRoleService.changeUserRole(currentAdmin, userId, request.getNewRole(), null);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.role.updated"),
                userMapper.build(updatedUser)));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status",
            description = "Activate or deactivate user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        User currentAdmin = getCurrentAdmin();
        String statusText = request.isActive() ?
                messageService.get("admin-rest-controller.user.status.active") :
                messageService.get("admin-rest-controller.user.status.inactive");

        log.info(messageService.get("admin-rest-controller.log.admin.changing.status",
                currentAdmin.getEmail(), userId, statusText));

        User updatedUser = userStatusService.toggleUserActive(currentAdmin, userId, request.isActive(), null);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.status.updated"),
                userMapper.build(updatedUser)));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user",
            description = "Delete user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId) {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.deleting.user",
                currentAdmin.getEmail(), userId));

        userDeletedService.deleteUserWithCheck(userId, currentAdmin, null);

        log.info(messageService.get("admin-rest-controller.log.user.deleted.success", userId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.deleted"),
                null));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID",
            description = "Get detailed information about a user")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long userId) {

        log.debug(messageService.get("admin-rest-controller.log.getting.user.by.id", userId));

        User user = userSearchService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.found"),
                userMapper.build(user)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users",
            description = "Search users by email or mobile number")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String identify,
            @RequestParam(required = false) String sort) {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.searching.users",
                currentAdmin.getEmail(), identify));

        List<User> users = userSearchService.searchUsersByCriteria(identify, identify, null, null, sort);

        if (users.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(
                    messageService.get("admin-rest-controller.user.search.not.found", identify),
                    List.of()));
        }

        List<UserResponse> userResponses = users.stream()
                .map(userMapper::build)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.search.results"),
                userResponses));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get statistics",
            description = "Get system usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {

        User currentAdmin = getCurrentAdmin();
        log.info(messageService.get("admin-rest-controller.log.admin.getting.stats", currentAdmin.getEmail()));

        UserStatistics stats = userSearchService.getUserStatistics(null);

        Map<String, Object> responseStats = new HashMap<>();
        responseStats.put(messageService.get("admin-rest-controller.stats.total"), stats.getTotalUsers());
        responseStats.put(messageService.get("admin-rest-controller.stats.active"), stats.getActiveUsers());
        responseStats.put(messageService.get("admin-rest-controller.stats.inactive"), stats.getInactiveUsers());
        responseStats.put(messageService.get("admin-rest-controller.stats.admins"), stats.getAdmins());
        responseStats.put(messageService.get("admin-rest-controller.stats.managers"), stats.getManagers());
        responseStats.put(messageService.get("admin-rest-controller.stats.users"), stats.getRegularUsers());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.stats.fetched"),
                responseStats));
    }

    private boolean hasSearchCriteria(String mobileNumber, String email,
                                      String firstName, String lastName) {
        return (mobileNumber != null && !mobileNumber.trim().isEmpty()) ||
                (email != null && !email.trim().isEmpty()) ||
                (firstName != null && !firstName.trim().isEmpty()) ||
                (lastName != null && !lastName.trim().isEmpty());
    }
}