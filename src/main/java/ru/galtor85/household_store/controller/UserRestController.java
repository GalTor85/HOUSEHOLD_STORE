package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.security.SecurityUser;
import ru.galtor85.household_store.service.MessageService;
import ru.galtor85.household_store.service.UserSearchService;
import ru.galtor85.household_store.service.UserService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API for Users")
public class UserRestController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserSearchService userSearchService;

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (SecurityUser) auth.getPrincipal();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information",
            description = "Retrieves the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        log.info(messageService.get("user-rest-controller.log.profile.fetch.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User user = userSearchService.getUserById(securityUser.getUserId());

        log.info(messageService.get("user-rest-controller.log.profile.fetch.success", user.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.fetch.success"),
                        userMapper.build(user)
                )
        );
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile",
            description = "Updates the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody UserEditRequest request) {

        log.info(messageService.get("user-rest-controller.log.profile.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.edit(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(messageService.get("user-rest-controller.log.profile.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.profile.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }

    @PutMapping("/password")
    @Operation(summary = "Update user password",
            description = "Updates the password of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request) {

        log.info(messageService.get("user-rest-controller.log.password.update.start"));

        SecurityUser securityUser = getCurrentSecurityUser();
        User updatedUser = userService.passwordUpdate(
                userSearchService.getUserById(securityUser.getUserId()),
                request
        );

        log.info(messageService.get("user-rest-controller.log.password.update.success", updatedUser.getId()));

        return ResponseEntity.ok(
                ApiResponse.success(
                        messageService.get("user-rest-controller.msg.password.update.success"),
                        userMapper.build(updatedUser)
                )
        );
    }
}