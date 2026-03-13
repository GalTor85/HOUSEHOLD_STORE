package ru.galtor85.household_store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.galtor85.household_store.advice.exception.*;
import ru.galtor85.household_store.dto.*;
import ru.galtor85.household_store.entity.User;
import ru.galtor85.household_store.mapper.UserMapper;
import ru.galtor85.household_store.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserSearchService userSearchService;

    private SecurityUser getCurrentSecurityUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException(
                    messageService.get("user-rest-controller.error.auth.header.missing"));
        }
        return (SecurityUser) auth.getPrincipal();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information",
            description = "Retrieves the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        log.info(messageService.get("user-rest-controller.log.profile.fetch.start"));

        try {
            SecurityUser securityUser = getCurrentSecurityUser();
            User user = userSearchService.getUserById(securityUser.getUserId());

            log.info(messageService.get("user-rest-controller.log.profile.fetch.success", user.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.profile.fetch.success"),
                            userMapper.build(user)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.fetch.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.profile.fetch.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile",
            description = "Updates the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> editUser(
            @Valid @RequestBody UserEditRequest request) {

        log.info(messageService.get("user-rest-controller.log.profile.update.start"));

        try {
            SecurityUser securityUser = getCurrentSecurityUser();
            User updatedUser = userService.edit(userSearchService.getUserById(securityUser.getUserId()), request);

            log.info(messageService.get("user-rest-controller.log.profile.update.success", updatedUser.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.profile.update.success"),
                            userMapper.build(updatedUser)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.update.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException | ValidationRequestException e) {
            log.warn(messageService.get("user-rest-controller.log.profile.update.validation", e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.profile.update.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }

    @PutMapping("/password")
    @Operation(summary = "Update user password",
            description = "Updates the password of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest request) {

        log.info(messageService.get("user-rest-controller.log.password.update.start"));

        try {
            SecurityUser securityUser = getCurrentSecurityUser();
            User updatedUser = userService.passwordUpdate(userSearchService.getUserById(securityUser.getUserId()), request);

            log.info(messageService.get("user-rest-controller.log.password.update.success", updatedUser.getId()));

            return ResponseEntity.ok(
                    ApiResponse.success(
                            messageService.get("user-rest-controller.msg.password.update.success"),
                            userMapper.build(updatedUser)
                    )
            );

        } catch (UnauthorizedException e) {
            log.warn(messageService.get("user-rest-controller.log.password.update.unauthorized", e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException | ValidationRequestException e) {
            log.warn(messageService.get("user-rest-controller.log.password.update.validation", e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error(messageService.get("user-rest-controller.log.password.update.error", e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(messageService.get("error.unexpected")));
        }
    }
}