package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.request.auth.UserCreateRequest;
import ru.galtor85.household_store.dto.request.user.UpdateRoleRequest;
import ru.galtor85.household_store.dto.request.user.UpdateStatusRequest;
import ru.galtor85.household_store.dto.response.order.RollbackApprovalDto;
import ru.galtor85.household_store.dto.response.system.ApiResponse;
import ru.galtor85.household_store.dto.response.user.UserResponse;
import ru.galtor85.household_store.dto.response.user.UserStatistics;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.mapper.user.UserMapper;
import ru.galtor85.household_store.mapper.user.UserToEntity;
import ru.galtor85.household_store.service.auth.AdminUserCreationService;
import ru.galtor85.household_store.service.i18n.LogMessageService;
import ru.galtor85.household_store.service.i18n.MessageService;
import ru.galtor85.household_store.service.rollback.RollbackService;
import ru.galtor85.household_store.service.user.UserDeletedService;
import ru.galtor85.household_store.service.user.UserRoleService;
import ru.galtor85.household_store.service.user.UserSearchService;
import ru.galtor85.household_store.service.user.UserStatusService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.galtor85.household_store.constants.EndpointConstants.CONTROL_ADMIN;
import static ru.galtor85.household_store.constants.PaginationConstants.DEFAULT_SORT_FIELD;

/**
 * REST controller for administrative user management operations.
 *
 * <p>This controller provides endpoints for administrators to:</p>
 * <ul>
 *   <li>List, search, and filter users</li>
 *   <li>Create new users with specific roles</li>
 *   <li>Update user roles and account status</li>
 *   <li>Delete user accounts</li>
 *   <li>View system user statistics</li>
 *   <li>Manage order rollback requests</li>
 * </ul>
 *
 * <p>All endpoints require ADMIN role for access.</p>
 */
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping(CONTROL_ADMIN)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "API for managing users as an admin")
public class AdminRestController extends BaseController {

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final UserSearchService userSearchService;
    private final UserRoleService userRoleService;
    private final UserStatusService userStatusService;
    private final AdminUserCreationService adminUserCreationService;
    private final UserDeletedService userDeletedService;
    private final UserToEntity userToEntity;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final RollbackService rollbackService;
    private final LogMessageService logMsg;

    private boolean hasSearchCriteria(String mobileNumber, String email,
                                      String firstName, String lastName) {
        return (mobileNumber != null && !mobileNumber.trim().isEmpty()) ||
                (email != null && !email.trim().isEmpty()) ||
                (firstName != null && !firstName.trim().isEmpty()) ||
                (lastName != null && !lastName.trim().isEmpty());
    }

    // =========================================================================
    // USER LISTING AND SEARCH
    // =========================================================================

    /**
     * Retrieves a list of all users with optional filtering and sorting.
     */
    @GetMapping
    @Operation(summary = "Get users list",
            description = "Get a list of all users with optional filtering and sorting")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @Parameter(description = "Filter by mobile number", example = "+1234567890")
            @RequestParam(required = false) String mobileNumber,
            @Parameter(description = "Filter by email", example = "user@example.com")
            @RequestParam(required = false) String email,
            @Parameter(description = "Filter by first name", example = "Max")
            @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by last name", example = "Pain")
            @RequestParam(required = false) String lastName,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortBy) {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.fetching.users",
                currentAdmin.getEmail(), currentAdmin.getId()));

        List<User> users;

        if (hasSearchCriteria(mobileNumber, email, firstName, lastName)) {
            users = userSearchService.searchUsersByCriteria(mobileNumber, email, firstName, lastName, sortBy);
            log.debug(logMsg.get("admin-rest-controller.log.searching.users.with.criteria",
                    email, mobileNumber, firstName, lastName));
        } else {
            users = userSearchService.getAllUsers(sortBy);
            log.debug(logMsg.get("admin-rest-controller.log.getting.all.users", sortBy));
        }

        List<UserResponse> userResponses = users.stream()
                .map(userMapper::build)
                .collect(Collectors.toList());

        log.info(logMsg.get("admin-rest-controller.log.returning.users", userResponses.size()));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.fetched"),
                userResponses));
    }

    /**
     * Searches for users by email or mobile number.
     */
    @GetMapping("/search")
    @Operation(summary = "Search users",
            description = "Search users by email or mobile number")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @Parameter(description = "Email or mobile number to search", example = "user@example.com", required = true)
            @RequestParam String identify,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sort) {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.searching.users",
                currentAdmin.getEmail(), identify));

        List<User> users = userSearchService.searchUsersByCriteria(identify, identify, null, null, sort);

        List<UserResponse> userResponses = users.stream()
                .map(userMapper::build)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.search.results"),
                userResponses));
    }

    /**
     * Retrieves detailed information about a specific user by ID.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID",
            description = "Get detailed information about a user")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(description = "User ID", example = "1", required = true)
            @PathVariable Long userId) {

        log.debug(logMsg.get("admin-rest-controller.log.getting.user.by.id", userId));

        User user = userSearchService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.found"),
                userMapper.build(user)));
    }

    // =========================================================================
    // USER CREATION
    // =========================================================================

    /**
     * Creates a new user account.
     */
    @PostMapping
    @Operation(summary = "Create user",
            description = "Create a new user")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.creating.user",
                currentAdmin.getEmail(), request.getEmail()));

        String creator = currentAdmin.getEmail() + " " + currentAdmin.getMobileNumber();
        User newUser = userToEntity.build(request, creator);

        User createdUser = adminUserCreationService.createUserWithRole(
                currentAdmin, newUser, request.getPassword(), request.getRole());

        log.info(logMsg.get("admin-rest-controller.log.user.created.success", createdUser.getEmail()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        messageService.get("admin-rest-controller.user.created"),
                        userMapper.build(createdUser)));
    }

    // =========================================================================
    // USER ROLE MANAGEMENT
    // =========================================================================

    /**
     * Updates the role of an existing user.
     */
    @PatchMapping("/{userId}/role")
    @Operation(summary = "Update user role",
            description = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @Parameter(description = "User ID", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.changing.role",
                currentAdmin.getEmail(), userId, request.getNewRole()));

        User updatedUser = userRoleService.changeUserRole(currentAdmin, userId, request.getNewRole());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.role.updated"),
                userMapper.build(updatedUser)));
    }

    // =========================================================================
    // USER STATUS MANAGEMENT
    // =========================================================================

    /**
     * Activates or deactivates a user account.
     */
    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status",
            description = "Activate or deactivate user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @Parameter(description = "User ID", example = "1", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        User currentAdmin = getCurrentUser();
        String statusText = request.isActive() ?
                messageService.get("admin-rest-controller.user.status.active") :
                messageService.get("admin-rest-controller.user.status.inactive");

        log.info(logMsg.get("admin-rest-controller.log.admin.changing.status",
                currentAdmin.getEmail(), userId, statusText));

        User updatedUser = userStatusService.toggleUserActive(currentAdmin, userId, request.isActive());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.status.updated"),
                userMapper.build(updatedUser)));
    }

    // =========================================================================
    // USER DELETION
    // =========================================================================

    /**
     * Permanently deletes a user account.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user",
            description = "Delete user by ID")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID", example = "1", required = true)
            @PathVariable Long userId) {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.deleting.user",
                currentAdmin.getEmail(), userId));

        userDeletedService.deleteUserWithCheck(userId, currentAdmin);

        log.info(logMsg.get("admin-rest-controller.log.user.deleted.success", userId));

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin-rest-controller.user.deleted"),
                null));
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Retrieves system user statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get statistics",
            description = "Get system usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {

        User currentAdmin = getCurrentUser();
        log.info(logMsg.get("admin-rest-controller.log.admin.getting.stats", currentAdmin.getEmail()));

        UserStatistics stats = userSearchService.getUserStatistics();

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

    // =========================================================================
    // ROLLBACK REQUEST MANAGEMENT
    // =========================================================================

    /**
     * Retrieves paginated list of pending rollback requests.
     *
     * @param page page number (0-indexed) - optional, uses config default if not provided
     * @param size page size - optional, uses config default if not provided
     * @return page of rollback approval DTOs
     */
    @GetMapping("/rollback-requests/pending")
    @Operation(summary = "Get pending rollback requests",
            description = "Retrieves a paginated list of pending rollback requests that need admin approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<RollbackApprovalDto>>> getPendingRollbacks(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(required = false) Integer size) {

        int effectivePage = getPage(page);
        int effectiveSize = getSize(size);

        log.debug(logMsg.get("admin-rest-controller.log.pagination",
                effectivePage, effectiveSize));

        Page<RollbackApprovalDto> approvals = rollbackService.getPendingRollbacks(effectivePage, effectiveSize);

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin.rollback.pending.fetched"),
                approvals));
    }

    /**
     * Approves a pending rollback request.
     *
     * @param approvalId the rollback approval ID
     * @param comments   admin comments for the approval
     * @return updated rollback approval DTO
     */
    @PutMapping("/rollback-requests/{approvalId}/approve")
    @Operation(summary = "Approve rollback request",
            description = "Approves a pending rollback request and executes the rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RollbackApprovalDto>> approveRollback(
            @Parameter(description = "Rollback approval ID", example = "1", required = true)
            @PathVariable Long approvalId,
            @Parameter(description = "Admin comments for approval", example = "Approved due to customer request")
            @RequestParam String comments) {

        User admin = getCurrentUser();
        RollbackApprovalDto approval = rollbackService.approveRollback(
                approvalId, comments, admin.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin.rollback.approved"),
                approval));
    }

    /**
     * Rejects a pending rollback request.
     *
     * @param approvalId the rollback approval ID
     * @param comments   admin comments explaining the rejection
     * @return updated rollback approval DTO
     */
    @PutMapping("/rollback-requests/{approvalId}/reject")
    @Operation(summary = "Reject rollback request",
            description = "Rejects a pending rollback request")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RollbackApprovalDto>> rejectRollback(
            @Parameter(description = "Rollback approval ID", example = "1", required = true)
            @PathVariable Long approvalId,
            @Parameter(description = "Admin comments explaining rejection", example = "Order already shipped")
            @RequestParam String comments) {

        User admin = getCurrentUser();
        RollbackApprovalDto approval = rollbackService.rejectRollback(
                approvalId, comments, admin.getId());

        return ResponseEntity.ok(ApiResponse.success(
                messageService.get("admin.rollback.rejected"),
                approval));
    }
}